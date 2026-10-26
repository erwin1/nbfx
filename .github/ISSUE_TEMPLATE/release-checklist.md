---
name: Release checklist
about: Track an nbfx release (see RELEASING.md)
title: "Release nbfx <version>"
labels: release
---
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
Release manager: @...
Version: ...
NetBeans revision (`nbfx.submodule.commit`): ...

### Prepare
- [ ] `nbfx.version` set in `nbfx.properties`
- [ ] `main` green: `ant bootstrap build`, `ant check-purity`, `ant rat`
- [ ] Release notes drafted
- [ ] Release manager's key is in `KEYS` and on the keyservers

### Release candidate (repeat per RC)
- [ ] Signed tag `<version>-rcN` pushed
- [ ] `ant release -Dnbfx.rc=rcN -Dnbfx.gpg.key=...` green
- [ ] Source release builds and runs from the unpacked tarball (`ant build run`)
- [ ] GitHub pre-release `<version>-rcN` with 6 files (src, bin, `.asc`, `.sha512`)
- [ ] Review window opened in this issue (at least 72 hours, until ...)

### Review
- [ ] +1 from Gluon
- [ ] +1 from the customer
- [ ] Result posted as a comment

### Release
- [ ] Signed tag `<version>` on the RC commit, pushed
- [ ] GitHub release `<version>` with the same 6 files (no rebuild)
- [ ] RC pre-release points to the final release
- [ ] Announced

### Afterwards
- [ ] `nbfx.version` bumped on `main`
