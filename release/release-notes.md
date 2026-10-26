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

    Release notes template. .github/workflows/release-draft.yml fills in the
    @...@ tokens and puts the result on the draft GitHub release.

-->
nbfx @VERSION@ — a pure-JavaFX IDE frontend on the Apache NetBeans runtime.

**This is not a release of the Apache Software Foundation.** nbfx is built on Apache NetBeans
(revision [`@NB_COMMIT@`](https://github.com/apache/netbeans/commit/@NB_COMMIT@)), which is.

**Requirements:** JDK 25. Bundles OpenJFX 27 (GPLv2 with the Classpath Exception, see `THIRDPARTY`), with
native libraries for macOS aarch64, Windows x64 and Linux x64.

| File | |
|---|---|
| `nbfx-@VERSION@-src.tar.gz` | the source release |
| `nbfx-@VERSION@-bin.zip` | convenience binary (unzip, run `bin/netbeans`) |
| `*.asc`, `*.sha512` | GPG signature and SHA-512 checksum of each file |

## Highlights

- _Release manager: summarise the release here._

## Known limitations

- No installers; the binary is a platform-independent zip.

## Verify

```sh
curl -LO https://raw.githubusercontent.com/@REPOSITORY@/@TAG@/KEYS
gpg --import KEYS
gpg --verify nbfx-@VERSION@-src.tar.gz.asc nbfx-@VERSION@-src.tar.gz
sha512sum -c nbfx-@VERSION@-src.tar.gz.sha512        # macOS: shasum -a 512 -c
```
