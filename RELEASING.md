<!--

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

-->
# Releasing nbfx

nbfx follows the Apache release procedure as closely as a non-ASF project can. The model is the
[Apache Answer release guide](https://answer.apache.org/community/how-to-release/) and the Apache NetBeans
release checklist ([apache/netbeans#9684](https://github.com/apache/netbeans/issues/9684)). Where nbfx
cannot yet do what Apache does, the step says so:

> **Apache:** what an ASF project does · **nbfx now:** what we do instead

Adopting the full procedure later should then be a small diff to this file, not a rewrite.

**nbfx is not an Apache Software Foundation project.** Artefacts, tags and announcements never use the
`apache-` prefix or the Apache name for nbfx itself.

## What a release consists of

| Artefact | Content |
|---|---|
| `nbfx-<version>-src.tar.gz` | **The release.** `git archive` of the release tag. It does not contain the apache/netbeans sources: the build fetches the pinned revision (`nbfx.submodule.commit` in `nbfx.properties`). |
| `nbfx-<version>-bin.zip` | Convenience binary: the nbfx cluster config (`ant build`), with LICENSE, NOTICE, THIRDPARTY and the bundled licence texts. Needs JDK 25. |
| `*.asc` | Detached, armored GPG signature of each artefact. |
| `*.sha512` | SHA-512 checksum of each artefact (`sha512sum -c` / `shasum -a 512 -c`). |
| `KEYS` | The public keys of the release managers (repository root). |

Versions are `MAJOR.MINOR.PATCH`. The tags are `<version>-rcN` for candidates and `<version>` for the
release, both on the same commit. Each RC is built under the final file names, so promoting it uploads the
same bytes again.

## One-time setup: the signing key

> **Apache:** the key is added to `https://dist.apache.org/repos/dist/release/<project>/KEYS` (svn) ·
> **nbfx now:** `KEYS` in this repository, and the key on public keyservers.

1. Create an RSA-4096 key (`gpg --full-generate-key`: "RSA and RSA", 4096, expiry `2y`) with your work address.
2. Publish it:
   ```sh
   gpg --keyserver keyserver.ubuntu.com --send-keys <FINGERPRINT>
   gpg --export <FINGERPRINT> | curl -T - https://keys.openpgp.org   # then confirm the e-mail
   ```
3. Append it to `KEYS` and commit:
   ```sh
   (gpg --list-sigs <FINGERPRINT> && gpg --armor --export <FINGERPRINT>) >> KEYS
   ```
4. Back up the secret key and the revocation certificate (`~/.gnupg/openpgp-revocs.d/`).

Extend an expiring key with `gpg --quick-set-expire <FINGERPRINT> 2y`, then re-publish it and replace its
`KEYS` entry.

## The procedure

Open a **release checklist** issue (template: *Release checklist*) and tick the steps off there.

### 1. Prepare

- Pick the version and set `nbfx.version` in `nbfx.properties`.
- Make sure `main` is green: `ant bootstrap build`, `ant check-purity`, `ant rat`.
- Write the release notes (template below).

### 2. Tag the release candidate

> **Apache:** RC tag + vote on the RC · **nbfx now:** the same.

```sh
git tag -s -a 0.1.0-rc1 -m "nbfx 0.1.0-rc1"
git push origin 0.1.0-rc1
```

### 3. Build, sign and verify

```sh
ant release -Dnbfx.rc=rc1 -Dnbfx.gpg.key=<FINGERPRINT>
```

This runs, in order:

| Target | Does |
|---|---|
| `release-src` | `git archive` of HEAD → `build/release/nbfx-<v>-src.tar.gz`. Refuses a dirty tree or a HEAD that is not the tag. |
| `release-bin` | `ant build`, the upstream licence summary (`create-mandatory-files-for-binary`), the nbfx LICENSE/NOTICE/THIRDPARTY additions, then `zip-cluster-config` → `build/release/nbfx-<v>-bin.zip`. |
| `release-sign` | `gpg --detach-sign --armor` → `.asc` and SHA-512 → `.sha512`, for each artefact. |
| `release-verify` | Verifies every `.asc` against a keyring that only contains `KEYS`, checks every `.sha512`, unpacks both archives, checks the licence files (including the JavaFX licence in the binary), checks that `javafx.web` is absent and runs Rat on the unpacked source release. |

`-Dnbfx.release.dryrun=true` skips the tag and clean-tree checks, for a rehearsal.

Then check by hand that the source release builds and runs:

```sh
tar xzf build/release/nbfx-0.1.0-src.tar.gz -C /tmp && cd /tmp/nbfx-0.1.0 && ant build run
```

### 4. Publish the candidate

> **Apache:** upload to `https://dist.apache.org/repos/dist/dev/<project>/` (svn) ·
> **nbfx now:** a GitHub **pre-release** on the RC tag.

Create a GitHub release for `0.1.0-rc1` and mark it as a pre-release. Attach the six files from
`build/release/` (two artefacts, each with `.asc` and `.sha512`) and paste the release notes.

### 5. Review

> **Apache:** `[VOTE]` on `dev@`, open for at least 72 hours, at least three binding +1 votes from the PMC,
> then `[RESULT][VOTE]` · **nbfx now:** a review of at least 72 hours on the release checklist issue; the
> release needs a +1 from Gluon and from the customer.

Reviewers verify the candidate (see *Verifying a release* below), try it, and comment `+1`, `0` or `-1`
with their reasons. A `-1` that holds means: fix on `main`, then go back to step 2 with `rc2`. When the
window closes, post the result (who voted what) as a comment.

### 6. Promote

> **Apache:** `svn mv` from `dist/dev` to `dist/release`, final tag · **nbfx now:** final tag and a GitHub
> release with the same files.

```sh
git tag -s -a 0.1.0 -m "nbfx 0.1.0" 0.1.0-rc1^{}
git push origin 0.1.0
```

Create the GitHub release `0.1.0` (not a pre-release) and upload the **same six files** from the RC. Do
not rebuild: the checksums must stay the same. Edit the RC pre-release to point to the final release.

### 7. Announce

> **Apache:** mail to `announce@apache.org` and `dev@` · **nbfx now:** the channels agreed with the
> customer, linking to the GitHub release.

### 8. Afterwards

- Bump `nbfx.version` on `main` to the next version.
- Close the checklist issue.

## Verifying a release

```sh
curl -LO https://raw.githubusercontent.com/gluonhq/nbfx/main/KEYS
gpg --import KEYS
for f in nbfx-*.tar.gz nbfx-*.zip; do gpg --verify "$f.asc" "$f"; done
for f in nbfx-*.sha512; do sha512sum -c "$f"; done      # macOS: shasum -a 512 -c "$f"
```

A good signature from a key in `KEYS` and `OK` for every checksum mean that the files are the ones the
release manager built. Then unpack the source release and check that it has `LICENSE`, `NOTICE` and
`THIRDPARTY`, that `ant rat` passes, and that `ant build run` works.

## Release notes template

```markdown
nbfx <version> — a pure-JavaFX IDE frontend on the Apache NetBeans runtime.

**This is not a release of the Apache Software Foundation.** nbfx is built on Apache NetBeans
(revision <nbfx.submodule.commit>), which is.

Requirements: JDK 25. Bundles OpenJFX 27 (GPLv2 with the Classpath Exception; see THIRDPARTY).
Native JavaFX libraries: macOS aarch64, Windows x64, Linux x64.

## Changes
- ...

## Known limitations
- No installers; the binary is a platform-independent zip.
- ...

## Verify
gpg --import KEYS && gpg --verify nbfx-<version>-src.tar.gz.asc nbfx-<version>-src.tar.gz
sha512sum -c nbfx-<version>-src.tar.gz.sha512
```
