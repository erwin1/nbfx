---
name: Release checklist
about: Track an nbfx release (see RELEASING.md)
title: "Release nbfx <version>"
labels: release
---
<!--

    Licensed to the Apache Software Foundation (ASF) under one
    or more contributor license agreements.  See the NOTICE file
    distributed with this work for additional information
    regarding copyright ownership.  The ASF licenses this file
    to you under the Apache License, Version 2.0 (the
    "License"); you may not use this file except in compliance
    with the License.  You may obtain a copy of the License at

      http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing,
    software distributed under the License is distributed on an
    "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
    KIND, either express or implied.  See the License for the
    specific language governing permissions and limitations
    under the License.

-->
Release manager: @...
Version: ...
NetBeans revision (`nbfx.submodule.commit`): ...

### Prepare
- [ ] `nbfx.version` set in `nbfx.properties`
- [ ] `main` green: `ant bootstrap build`, `ant check-purity`, `ant rat`
- [ ] `release/release-notes.md` up to date
- [ ] Release manager's key is in `KEYS` and on the keyservers

### Release candidate (repeat per RC)
- [ ] Signed tag `<version>-rcN` pushed; *Release draft* green
- [ ] `ant release -Dnbfx.rc=rcN -Dnbfx.gpg.key=...` green
- [ ] Binary starts on each platform with an empty userdir (RELEASING.md §3)
  - [ ] macOS aarch64
  - [ ] macOS x64
  - [ ] Windows x64
  - [ ] Linux x64
- [ ] 6 files uploaded to the draft (`gh release upload <version>-rcN build/release/*`)
- [ ] *Release verify* green (`gh workflow run release-verify.yml -f tag=<version>-rcN`)
- [ ] Highlights written, pre-release published
- [ ] Review window opened in this issue (at least 72 hours, until ...)

### Review
- [ ] +1 votes from different contributors
- [ ] Result posted as a comment

### Release
- [ ] Signed tag `<version>` on the RC commit, pushed; *Release draft* green (copies the RC files)
- [ ] Draft release `<version>` checked and published
- [ ] RC pre-release points to the final release
- [ ] Announced

### Afterwards
- [ ] `nbfx.version` bumped on `main`
