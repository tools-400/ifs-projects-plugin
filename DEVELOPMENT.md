# IFS Projects – Developer notes

How the plug-in works and how it is built. For its use, see [README.md](README.md).

## Setup

* Clone the repository into the RDi workspace. The target platform is the running platform (RDi).
* Java 8 byte code, so that the plug-in runs in RDi 9.6.
* Launch configurations for RDi 9.8 and 9.9 (German and English):
  `eclipse/IFS Projects Core Plugin/launches`. The 9.9 configurations set
  `-Dcom.ibm.cic.licensing.p2.home.dir=C:\ProgramData`; without it the license check of the IBM i
  connection fails (RDi 9.8 has it in `config.ini`).
* Unit tests: fragment `eclipse/IFS Projects JUnit` (*Run As > JUnit Test*).
* Update site: *Build All* in `eclipse/IFS Projects Update Site/site.xml`.
* Release: `build/IFS Projects Build/build/build.xml` (same script as iSphere, iRPGUnit and
  JCRCMDS, settings in `build.properties`). Update site and home page go to `docs/` of the
  repository `ifs-terminal` (GitHub Pages).

## Architecture

Packages below `de.tools400.ifsprojects`:

| Package | Content |
|---|---|
| `subsystem` | Subsystem *IFS Projects*. Delegates its filter strings (`/directory/*`) to the IFS subsystem of IBM instead of extending it. Connector service from `ToolboxConnectorServiceManager`. |
| `terminal` | `ITerminalLauncher`, the only interface the handlers know. `launch()` returns an `ITerminalCommandLine`, which tells whether the session has been closed since. Backends come from the extension point `terminalLaunchers`; the available one with the highest priority is used. |
| `terminal.tm` | Backend for the TM Terminal of RDi 9.6 - 9.9: a view around `TerminalViewControlFactory.makeControl()`. `TmTerminalView` with `TerminalActionGroup` (actions) and `TerminalRequestMemento` (restore), `TerminalSession` with `TerminalKeyHandler` (keys); calls, that differ between the TM versions, in `TmCompatibility`. The SSH connector is created from the extension registry, because extension points and connector IDs differ between the TM versions. A backend for `org.eclipse.terminal.*` can be added as a separate bundle. |
| `build` | Terminal: `TerminalBuildCommands`, `TerminalBuildJob`. RSE job: `RseBuildCommands`, `RseBuildJob`, `CommandsLog`, `LogLines`. Both: `BuildEvents`, the errors are shown by `errorlist.ErrorList`. Started by `handlers.BuildLauncher` (build mode of the preferences, placeholders of the compile command) via `handlers.TerminalBuildLauncher` or `handlers.RseBuildLauncher`. |
| `syntaxcheck` | `SyntaxCheckCommands`, `SyntaxCheckRunner`, `SyntaxCheckJob`. Started by `handlers.SyntaxCheckLauncher` (context menu and LPEX action `lpex.CheckSyntaxAction`). |
| `remote` | `RemoteJobs`: IBM Toolbox helpers of build and syntax check, paths in the IFS. `RemoteSession`: copy of the system object of RSE for a job, with its command call and temporary files, closed with try-with-resources. |
| `errorlist` | `ErrorList`: shows the events of builds and syntax checks in the *Error List* view of RDi (parser and view of RDi); the local events file is written in a workspace job, the view is updated in a UI job. `EventsFiles`: content of events files. |
| `ibmi` | Syntax of the IBM i, without dependencies on the other packages: `ClCommands` (CL literals, object names, paths of libraries, files and members), `PaseCommands` (shell quoting, placeholders, `QP2SHELL2`), `IfsPaths`. |
| `rse` | RSE helpers: `RseSelections` (host and directory of the selection), `RseConnections` (lookup of a connection), `RseTerminalRequests` (terminal request of a connection, may prompt for the user ID), `IfsFilterStrings`, `RemoteEditors`. |
| `local` | Local IFS projects: `LocalProject` (settings, `iproj.json`), `Deployment` (upload), `DeployManifest`, `SourceMapping`, `EventsFileMapper`. The launchers pass the `SourceMapping` through the builds to `errorlist.ErrorList`. |
| `project` | `iproj.json`: `IProjConfig`, `IProjValues` (values with resolved variables), `IProjFileStore` (IFS). `EnvironmentVariables` (also their export before a command), `BuildTool`. `ProjectFiles` and `IgnoreRules`: the files of the upload and of the export. |
| `project.propertypages` | `AbstractIProjPropertyPage` and `AbstractEnvironmentPropertyPage` for IFS projects and local IFS projects. `LocalProjectSettingsGroup` for property page and wizards of local IFS projects. `IProjToolTips` for property pages and wizards. |
| `project.wizards` | `IProjDefaultsGroup` (new `iproj.json`) for all wizards. `AbstractLocalIfsProjectWizard` creates the local IFS project for *New* and *Import* (undoable); both end with `NewLocalIfsProjectWizardPage`. *Import* uses an existing `.project` like *Existing Projects into Workspace*. |
| `export` | Export to a library: `ExportMapping` (stream file to member), `MemberText`, `IncludeResolver` and `IncludeConverter` (`RpgIncludeConverter`, `ClIncludeConverter`), `SourceCheck`, `LibraryInfo`, `ExportPlan` (preview) computed by `ExportAnalyzer`, `ExportSettings`, `ExportCommands`, `ExportJob`. The project is an `ExportSource`: `IfsProjectExportSource` or `local.LocalProjectExportSource`. |
| `export.wizards` | `ExportToLibraryWizard` with `ExportSettingsWizardPage` and `ExportPreviewWizardPage`. Started by `handlers.ExportIfsProjectHandler` and `handlers.ExportLocalProjectHandler`. |
| `preferencepages` | `IfsProjectsPreferencePage` (build) with the sub-pages `SshTerminalPreferencePage` and `ExportPreferencePage`. Field editors `EnvironmentVariablesFieldEditor` and `CcsidFieldEditor`. |
| `swt` | `ToolTips` and `CcsidCombo` (CCSID of new source files, used by the preferences and the export). |
| `utils` | `PlatformUtils`: shell of the active workbench window, the parent of the dialogs of jobs. `StringUtils`: `trimToNull`, `isBlank`, `nullToEmpty`. |

The `*Commands` classes have no RDi dependency. Neither have `ExportMapping`, `MemberText`,
`IncludeResolver`, `IncludeConverter`, `SourceCheck` and `ExportPlan`, which only use IBM Toolbox
for Java.

## IBM i

* Upload, builds over RSE, syntax check and the polling of terminal builds use IBM Toolbox for
  Java with a copy of the RSE system object. Commands run in a remote command server job of
  their own.
* All files are stored below `<home>/.ifsprojects` (`HOMEDIR` of the user profile, retrieved once
  per system and user):
  * `tmp`: `ifsprojects.sh` and the temporary files `ifsprojects_<id>.*`, deleted after 24 hours.
  * `builds/<project>`: default deploy directories of local IFS projects.

## SSH terminal

* The VT100 terminal and the SSH connector (JSch) of the Eclipse TM Terminal, which is part of
  RDi 9.6 - 9.9. The plug-in provides the view: session list, restore, reuse, sending command
  lines.
* Keys: a display filter of `TerminalKeyHandler` handles Ctrl+C (copy the selection and clear it),
  Ctrl+V, Shift+Page Up/Down and Ctrl+Shift+Home/End before TM. The TM bindings (Ctrl+Shift+C/V,
  Ctrl+Insert/Shift+Insert) still work. On macOS, Ctrl+C and Ctrl+V go to the shell.
* A connected terminal with the focus receives all keys, not the key bindings of the workbench.
  RDi 9.9 does so only while the mouse pointer is over the terminal;
  `TerminalKeyHandler.updateKeyCapture()` makes it follow the focus.
* RDi 9.9 stops the display updates on disconnect and does not restart them on reconnect;
  `TmCompatibility.restartDisplayUpdates()` does.
* Shutdown: unless *Restore terminals after a restart* is set, `TmTerminalLauncher` closes the
  sessions. The view stays open, so that the workbench restores it like other views. Password and
  command are never saved.

## Build in the terminal

The terminal does not report the end of a command. Therefore the command is wrapped in a subshell,
which writes marker files:

```
cd <project> && <exports> && ( _ifsp=<id>; . '<home>/.ifsprojects/tmp/ifsprojects.sh'; <command>; _ifsp_end $? )
```

* The shell of the terminal parses the command, so its aliases, functions and variables work.
  `cd` and the exports stay in effect.
* Start file: process ID of the subshell; its modification time is the start time. Exit code file:
  written at the end and by traps (129 terminal closed or connection lost, 130 Ctrl+C, 143
  SIGTERM).
* Not usable: `; echo $? > file` (bash skips it after Ctrl+C) and an EXIT trap (bsh does not run
  it in a subshell, bash and ksh report 0 after a signal). Verified with bash, ksh and bsh.
* A job (*Progress* view) polls the marker files every second. It ends, if
  * the terminal is closed before the start file exists (the shell has not read the command),
  * the command has not started after 60 seconds (e.g. missing directory, input on the prompt
    line),
  * the subshell is gone without exit code file (`kill -9`; checked every 30 seconds with `ps -p`
    via `QP2SHELL2`).

  Canceling the job only stops waiting. Ctrl+C in the terminal interrupts the build.
* While the job waits, the next build of the project opens a new terminal: the busy shell would
  read the command only after the running one and discard it after Ctrl+C. Verified with bash,
  ksh and bsh.
* Builds reuse only build terminals (reuse key `build:<connection>:<directory>`, which is saved with
  the terminals), never terminals of the user. A disconnected build terminal is connected
  again and receives the command, once the shell is ready.
* `ifsprojects.sh` is kept and only written, if it is missing or differs. The job deletes the
  marker files.
* A command that contains a comment (`#` at the beginning of a word) or ends with `&` or `|`
  cannot be wrapped. It runs unchanged, without *Error List*.

## Build in a job of the RSE connection

* No SSH login: `QP2SHELL2` starts `/QOpenSys/pkgs/bin/bash` (else `/QOpenSys/usr/bin/sh`). The
  job has no `HOME`, so the script exports it and runs `/QOpenSys/etc/profile` and
  `.bash_profile`/`.profile` like a login shell, so that `makei` and `gmake` are found. Then it
  runs `cd <project> && <exports> && <command>`.
* The output goes to a log file in `<home>/.ifsprojects/tmp`, which is read every 500 ms and
  written line by line to the *Commands Log* view (`QSYSCommandSubSystem.logExplanation()`).
  `QP2SHELL2` does not return the exit code, so the script writes it to a file.
* Afterwards the *Error List* shows the events files changed since the start: `.evfevent/` of the
  project (TOBi), otherwise the members of `EVFEVENT` in `objlib` or `curlib` of `iproj.json`
  (gmake).
* Builds of the same project run one after the other.
* *Cancel*: the script writes the process ID of the shell to a file. A second job collects the
  shell and all its descendants with `ps` and ends them with `kill -TERM` (`makei`, `make`, the
  `system` calls of the compiles, which run in jobs of their own). A running compile is aborted;
  TOBi may leave `.Rules.mk.build` in the project directory.

## Syntax check

*Check Syntax* compiles an RPGLE or SQLRPGLE stream file with `OPTION(*EVENTF)` and shows the
events file in the *Error List*.

* The job is set up like a TOBi build: current directory = project directory, `setIBMiEnvCmd`,
  `curlib`, `preUsrlibl`, `postUsrlibl` (`&NAME` resolved from the environment variables).
  `includePath` (relative to the project directory) is passed as `INCDIR`.
* RPGLE: `CRTBNDRPG ... DFTACTGRP(*NO)`, for `NOMAIN` sources `CRTRPGMOD`, with
  `OPTION(*NOGEN *EVENTF)`: no object is created.
* SQLRPGLE: `CRTSQLRPGI OBJTYPE(*MODULE) RPGPPOPT(*LVL2)` into `QTEMP`. `*NOGEN` would skip the
  RPG compiler, `COMPILEOPT('OPTION(*NOGEN)')` fails with CPD0042. `RPGPPOPT(*LVL2)` makes the
  host variables of copy books known to the precompiler. RNF1324 (DFTACTGRP not allowed for
  modules) is removed from the events file.
* UTF-8 sources need `TGTCCSID(*JOB)` and `CVTCCSID(*JOB)` (RNS9339, SQL8032).
* Unsaved changes are saved first, if the user agrees. The check waits until RSE has uploaded the
  file.
* The events file is copied to `<home>/.ifsprojects/tmp`, read and deleted (`RemoteSession.readMember()`). As in RDi, its local
  copy is stored in the project `RemoteSystemsTempFiles` (folder `IFSProjects`), one file per
  stream file, so that a new check replaces its table in the *Error List*. RSE does not upload it,
  because it has no remote file properties.
* The project must not be hidden: the view finds new markers via resource deltas, which skip
  hidden resources. As in RDi, the view is opened before the markers are created; otherwise a new
  view asks whether to change the message style.
* `EventsFileParser` and `QSYSErrorListViewPart` of RDi exist in RDi 9.6 - 9.9.
* LPEX user action: the plug-in does not change the LPEX profile. It only contributes to the
  extension point `com.ibm.lpex.preload`, so that LPEX can load its classes.

## Local IFS projects

* Settings and environment variables (per connection) are persistent properties of the project
  (at most 2048 characters each), not in `.project` or `.settings`, because they are user
  specific.
* Upload: the project is refreshed first, so that changes made by git are uploaded. A manifest in
  the state location of the plug-in (`deploy/<SHA-1 of connection, deploy directory and
  project>.manifest`) records size, modification time, CCSID and MD5 hash of each file. A file is
  uploaded again, if one of them has changed; if only size or time differ, the hash decides (e.g.
  after `git checkout`).
* Delete: files of the manifest that are gone or excluded now are deleted in the deploy
  directory, and so are directories that become empty. Deletes run first, because a file renamed
  in case only is the same stream file. Other files, e.g. the output of TOBi, are never deleted.
* `.ifsprojects-deploy` in the deploy directory contains the token of the manifest. A new token is
  written before the directory is changed. If the tokens differ (another workspace, Code for
  IBM i, someone else), the manifest is not trusted and all files are uploaded.
* If they match, the stream files are listed (one list operation per directory) and compared with
  size and modification time of the last upload. Files changed in the IFS are overwritten or
  deleted, if the user agrees; *No* cancels the upload. Missing stream files are uploaded again.
  Verified: `listFiles()` returns size and modification time (milliseconds) like a direct access.
* Excluded: the files of `.deployignore` or, if it does not exist, `.gitignore` of the project
  directory (git syntax: `#`, `!`, trailing `/`, leading or inner `/`, `*`, `?`, `[...]`, `**`;
  ignore files in subdirectories are not read). Never uploaded: derived, team private and hidden
  resources, `.git`, `.project`, `.settings`, `.deployignore`, `.ifsprojects-deploy` and the
  output directories of TOBi `.logs`, `.evfevent`, `.deps`.
* The bytes are copied unchanged. A new stream file gets the CCSID of the local encoding (UTF-8
  1208, windows-1252 1252, ISO-8859-1 819, ISO-8859-15 923, US-ASCII 367, otherwise 1208); a
  stream file with another CCSID is replaced. The modification time is the upload time, so that
  make rebuilds every uploaded change. Verified: CCSID and bytes of new, overwritten and replaced
  files.
* Errors: `local.EventsFileMapper` replaces the paths of the deploy directory in the events files
  by `/<project>/<relative path>` (FILEID length adjusted, FILEIDCONT merged, case ignored,
  because the compilers write e.g. `/home/Jsmith`) and sets the marker attribute `projectName`.
  Then the *Error List* opens the local file in LPEX
  (`ISeriesCompileEditManager.getOtherProjectFileEditor()`, RDi 9.5 - 9.9). Other files, e.g.
  system include files, keep their IFS path.
* Limitation of RDi: the LPEX outline does not find copy books in the `includePath` directories
  (`/COPY file ... not available`); compile and syntax check are not affected. The RPG parser
  searches a relative `/copy` in the current directory of the IFS subsystem, the global include
  directories (*Preferences > Program Verifiers > ILE RPG*), `RPGINCDIR` of the RSE job, the
  directory of the source and, for local files, the directory of the source in the project and
  the root of referenced projects. There is no per-project setting that the plug-in could fill.
  According to the code of RDi 9.8 (not tested), the outline finds `/copy inc/name.rpgleinc` of a
  source in the project directory.

## Export to a library

*Export to Library...* copies the source files of an IFS project or a local IFS project into
source members, e.g. for a change management system. The facts below were verified on the IBM i.

* The wizard reads the files of the project once (`ExportSource.listFiles()`, `readFile()`) and the
  library for every new library name (`LibraryInfo`). Then it computes the `ExportPlan` in memory;
  deselecting a file computes it again. The job exports exactly the content of the plan.
* Files: IFS projects are listed with `IFSFile.listFiles()` and read with the CCSID of the stream
  file. Local IFS projects are refreshed and read from the workspace. Both skip the files that the
  upload skips (`ProjectFiles`, `IgnoreRules`). The deploy directory of a local IFS
  project is only used to resolve absolute include paths and `includePath`; it is not changed.
* Library: `OBJECT_STATISTICS(lib, '*FILE')` joined with `SYSTABLES` (`FILE_TYPE` `S`) and
  `SYSCOLUMNS` (length and CCSID of `SRCDTA`) over the JDBC driver of IBM Toolbox for Java, the
  members from `SYSPARTITIONSTAT`. Program-described files, e.g. `EVFEVENT`, are not in
  `SYSTABLES`.
* Commands (`ExportCommands`) in a remote command server job of its own: `CRTLIB` and `CRTSRCPF`,
  if needed. Per member, the content is written to the temporary stream file
  `<home>/.ifsprojects/tmp/ifsprojects_<id>.export` with CCSID 1208 and copied with
  `CPYFRMSTMF MBROPT(*REPLACE)`. `CPYFRMSTMF` creates a missing member without source type and
  text and keeps both of an existing member. Therefore a new member is added before with `ADDPFM`
  (and removed with `RMVM`, if the copy fails), a replaced member gets its source type and text
  with `CHGPFM` afterwards.
* Lines: `CPYFRMSTMF` counts characters, not bytes, expands tabs to tab stops of 8 and drops a
  byte order mark. Lines longer than the record are truncated with the message CPIA083, which is
  also sent, if only trailing blanks are lost. Characters that do not exist in the CCSID of the
  source file become X'3F' without any message. Therefore `SourceCheck` checks the lengths and the
  characters (round trip with `CharConverter`) before the export, instead of evaluating CPIA083.
* CCSID of new source files: only single-byte EBCDIC CCSIDs (space X'40', U+3042 converts to one
  byte). `CRTSRCPF` rejects ASCII and Unicode CCSIDs except 1208, and lines with double-byte
  characters or in UTF-8 are longer than their number of characters.
* Includes (`IncludeResolver`, verified with `CRTBNDRPG` and `CRTBNDCL`):
  * RPG searches a relative name in the current directory (the project directory for TOBi), the
    `INCDIR` directories and the directory of the including file. In each directory it tries the
    name as it is, with `.rpgleinc` and with `.rpgle`. A simple name, that is not found in the IFS,
    is the member `QRPGLESRC,NAME` of the library list. Each copy book needs its own `**FREE`.
  * CL searches the directory of the including file and the `INCDIR` directories, not the current
    directory.
* `RpgIncludeConverter`: fixed format from column 7, the columns 81 and later keep their
  position, compile-time data after the first `**` line are not changed. `ClIncludeConverter`:
  only `INCLUDE SRCSTMF()` on one line. The target is written in the case of the keyword, e.g.
  `/copy inc,consts`.
* A file, whose lines are too long for an existing source file, is not exported. The plan
  converts the includes again, until no further file fails, so that no include refers to a missing
  member.
* Settings: IFS projects in the property set `de.tools400.ifsprojects.export` of the filter, local
  IFS projects in the persistent property `export.<connection>` (format of the environment
  variables). Defaults: the preferences `exportCcsid` and `exportRecordLength` and the library
  `objlib` or `curlib` of `iproj.json`.
* Context menus: IBM, iSphere and Eclipse put their actions into the group `additions` too.
  Therefore the commands of the plug-in are placed at the end of the group
  (`before=group.team` in the Remote Systems view, `before=group.properties` in the other views)
  behind a separator. JFace merges the separator with the one of the next group, if none of the
  commands is visible.
