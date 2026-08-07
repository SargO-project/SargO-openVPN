#!/usr/bin/env bash
#
# Refuses a SargO OpenVPN release APK that is not what the signing job claims to have produced.
#
# WHY THIS EXISTS. The Gradle build falls back to a DEBUG signature when the keystore properties
# are absent — correct for a developer, and the reason the old workflow could upload a debug-signed
# artifact named `...-release-apk` without anything going red. The workflow now fails closed on a
# missing secret; this asserts the other end, on the ARTIFACT rather than on the configuration,
# because a config check only proves what we wrote and not what came out.
#
# It is deliberately smaller than the MDM repository's assert-plugin-apk.sh: there is no versionCode
# band scheme here and no per-plugin targetSdk to pin. What it does share is the property that
# matters most — the signer is compared against a fingerprint the repository states, not against
# "whatever signed it".
#
# Required env:
#   APK              (optional) path to the APK; discovered under main/build/outputs/apk if unset
#   EXPECTED_SIGNER  SHA-256 certificate fingerprint, colons and case ignored
#   EXPECTED_APP_ID  the applicationId the artifact must declare
# Plus ANDROID_SDK_ROOT or ANDROID_HOME.
set -euo pipefail

fail() {
  echo "::error::assert-sargo-apk: $1" >&2
  exit 1
}

for v in EXPECTED_SIGNER EXPECTED_APP_ID; do
  [ -n "${!v:-}" ] || fail "$v is not set. It is a repository variable, not a secret — a certificate fingerprint is public data — and an unset one would make this script compare against the empty string and pass"
done

SDK_ROOT="${ANDROID_SDK_ROOT:-${ANDROID_HOME:-}}"
[ -n "$SDK_ROOT" ] || fail "neither ANDROID_SDK_ROOT nor ANDROID_HOME is set"

# Newest build-tools present, rather than a pinned version: this repository does not carry one, and
# apksigner/aapt2 read an APK the same way across versions.
BT="$(find "$SDK_ROOT/build-tools" -maxdepth 1 -mindepth 1 -type d | sort -V | tail -1)"
[ -n "$BT" ] && [ -x "$BT/apksigner" ] || fail "no usable build-tools found under $SDK_ROOT/build-tools"

if [ -z "${APK:-}" ]; then
  MATCHES="$(find main/build/outputs/apk -name '*universal*.apk' -type f 2>/dev/null || true)"
  COUNT="$(printf '%s\n' "$MATCHES" | grep -c . || true)"
  [ "$COUNT" -eq 1 ] || fail "expected exactly one universal APK, found $COUNT:"$'\n'"$MATCHES"
  APK="$MATCHES"
fi
[ -f "$APK" ] || fail "APK not found at $APK"

WORK="${RUNNER_TEMP:-/tmp}/sargo-apk-assert"
rm -rf "$WORK"; mkdir -p "$WORK"

# --- 1. signed by the expected key ------------------------------------------------------------
"$BT/apksigner" verify --verbose --print-certs "$APK" > "$WORK/verify.txt" 2>&1 \
  || fail "apksigner could not verify $APK:"$'\n'"$(cat "$WORK/verify.txt")"

ACTUAL_SIGNER="$(awk -F':' '/^Signer #1 certificate SHA-256 digest/ { print $2 }' "$WORK/verify.txt" | tr -d ' :' | tr '[:upper:]' '[:lower:]')"
EXPECTED_NORM="$(printf '%s' "$EXPECTED_SIGNER" | tr -d ' :' | tr '[:upper:]' '[:lower:]')"
[ -n "$ACTUAL_SIGNER" ] || fail "could not read a signer fingerprint from apksigner output"
[ "$ACTUAL_SIGNER" = "$EXPECTED_NORM" ] \
  || fail "signer mismatch — expected $EXPECTED_NORM, got $ACTUAL_SIGNER. A debug-signed build reaches this line looking exactly like a release one"

# --- 2. v2/v3 present -------------------------------------------------------------------------
# v3 carries the certificate lineage, which is the only mechanism that allows the signing key to be
# rotated later without uninstalling from every device.
grep -q '^Verified using v2 scheme.*true' "$WORK/verify.txt" || fail "the APK is not v2-signed"
grep -q '^Verified using v3 scheme.*true' "$WORK/verify.txt" || fail "the APK is not v3-signed; without v3 the signing key can never be rotated"

# --- 3-4. badging-derived ---------------------------------------------------------------------
"$BT/aapt2" dump badging "$APK" > "$WORK/badging.txt"
[ -s "$WORK/badging.txt" ] || fail "aapt2 produced no badging output"

ACTUAL_APP_ID="$(sed -n "s/^package: name='\([^']*\)'.*/\1/p" "$WORK/badging.txt")"
[ "$ACTUAL_APP_ID" = "$EXPECTED_APP_ID" ] \
  || fail "applicationId mismatch — expected $EXPECTED_APP_ID, got $ACTUAL_APP_ID"

grep -q "^application-debuggable" "$WORK/badging.txt" \
  && fail "the APK is debuggable; a debuggable build must never be signed with the Device Owner key"

# --- 5. it installs on the lowest API it claims -----------------------------------------------
MIN_SDK="$(sed -n "s/^sdkVersion:'\([0-9]*\)'.*/\1/p" "$WORK/badging.txt")"
[ -n "$MIN_SDK" ] || fail "could not read minSdkVersion from badging output"
"$BT/apksigner" verify --min-sdk-version "$MIN_SDK" "$APK" >/dev/null 2>&1 \
  || fail "the APK declares minSdkVersion $MIN_SDK but does not verify at that level — a device on the lowest API it claims to support cannot install it"

rm -rf "$WORK"
echo "assert-sargo-apk: OK — $ACTUAL_APP_ID, signer $ACTUAL_SIGNER, v2+v3, non-debuggable, verifies at API $MIN_SDK"
