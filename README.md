# IFS Projects

RDi plug-in for SSH terminals from the *Remote Systems* view and for IFS projects (`iproj.json`),
which are built with gmake or TOBi (`makei`).

Supported versions: RDi 9.6, 9.8 and 9.9.

## SSH terminals

| Selected object | Menu item | Starting directory |
|---|---|---|
| IBM i connection | *Open SSH Terminal* | home directory of the user |
| IFS directory | *Open SSH Terminal Here* | the directory |
| IFS file | *Open SSH Terminal Here* | the parent directory |
| IFS filter with one filter string | *Open SSH Terminal Here* | the directory of the filter string |

* The *SSH Terminal* view shows the active terminal on the left and the list of all terminals on
  the right. A terminal of the same connection and directory is reused; *Open New SSH Terminal ...*
  or the *New Terminal* button opens another one.
* Keys in the terminal: Ctrl+C copies the selected text and clears the selection; without a
  selection it interrupts the command. Ctrl+V pastes. Ctrl+Shift+C/V and Ctrl+Insert/Shift+Insert
  of the TM terminal still work. Shift+Page Up/Down scrolls back and forward by one page,
  Ctrl+Shift+Home/End to the beginning or end of the buffer. On macOS, Ctrl+C and Ctrl+V go to the
  shell; Cmd+C and Cmd+V copy and paste.
* While the terminal has the focus and is connected, it receives all keys instead of the key
  bindings of RDi, e.g. Ctrl+A, Ctrl+E or Ctrl+R for the shell. The TM terminal of RDi 9.9 does so
  only while the mouse pointer is over the terminal; the plug-in makes it follow the focus.
* Up/Down browse the command history of the shell. bsh, the default shell of the IBM i, has
  neither command history nor line editing. Make bash (open source package) the login shell of
  your user profile with `CALL QSYS2.SET_PASE_SHELL_INFO('*CURRENT', '/QOpenSys/pkgs/bin/bash')`
  (`*DEFAULT` instead of `*CURRENT`: all users without a shell of their own, requires *SECADM).
* SSH keys are configured at *Preferences > General > Network Connections > SSH2*. JSch supports
  RSA and ECDSA keys in PEM format, but no Ed25519 keys.

## IFS projects

Each IBM i connection has a subsystem *IFS Projects*. Each of its filters is a project directory,
whose files can be opened, edited and compared as usual.

| Action | Function |
|---|---|
| *New > IFS Project...* | Creates the project, optionally the directory and `iproj.json`. An existing `iproj.json` is used unchanged. |
| *Build* (project) | Runs `buildCommand` of `iproj.json` or the default build command, in the terminal of the project or in a job of the RSE connection (preferences). Afterwards the errors of the compiled objects are shown in the *Error List* view. |
| *Build* (file) | Runs `compileCommand` of `iproj.json` or the default compile command, like *Build* (project). |
| *Check Syntax* (RPGLE, SQLRPGLE file) | Compiles the file into `QTEMP` and shows the errors in the *Error List* view of RDi, see below. |
| *Open SSH Terminal in Project Directory* | |
| *Properties > IFS Project* | Edits `iproj.json`. Unknown attributes are kept. |
| *Properties > IFS Project > Environment* | Environment variables of the project. |

Placeholders of the compile command: `{filename}` and `{files}` (name of the file), `{host}`,
`{usrprf}`.

Environment variables hold system specific settings. They are stored in the project filter, not
in the project, and exported before each build and compile command. TOBi resolves `&NAME` in
`iproj.json`, e.g. `"setIBMiEnvCmd": ["&ENVCMD"]` with
`ENVCMD=CHGSYSLIBL LIB(UBMSYS) OPTION(*REMOVE)`.

### Build in the terminal

With *Preferences > IFS Projects > Run build and compile commands > In the SSH terminal of the
project* (default), *Build* passes the command line to the terminal of the project. The terminal
shows the output. Afterwards the errors of the compiled objects are shown in the *Error List* view,
collected like after a build in a job of the RSE connection (see below).

* The plug-in does not learn from the terminal, when the command ends. Therefore the command is
  wrapped in a subshell, that sources the script `ifsprojects.sh` in `<home>/.ifsprojects/tmp`
  (see *Files in the IFS*) and writes marker files:
  `cd <project> && <exports> && ( _ifsp=<id>; . '<home>/.ifsprojects/tmp/ifsprojects.sh'; <command>; _ifsp_end $? )`.
  The shell of the terminal parses the command, so that its aliases, functions and variables are
  available. `cd` and the exports stay in effect as before.
* The start file contains the process ID of the subshell. Its modification time is the start time
  of the build. The exit code file is written at the end and by traps: 129, if the terminal is
  closed or the connection is lost, 130 for Ctrl+C, 143 for SIGTERM. A command after the build
  command (`; echo $? > file`) is skipped by bash after Ctrl+C. An EXIT trap cannot be used: bsh
  does not run it in a subshell, bash and ksh report exit code 0 after a signal. Verified with
  bash, ksh and bsh of the IBM i.
* A job (*Progress* view) polls the marker files every second with a copy of the RSE system object.
  If the terminal is closed before the start file exists, the job ends, because the shell has not
  read the command line. The job also ends, if the command has not started 60 seconds after it has
  been written to the terminal, e.g. because the project directory does not exist or the prompt
  line contained input. A killed subshell (`kill -9`) writes no exit code file, therefore the job
  checks every 30 seconds with `ps -p` (via `QP2SHELL2`), whether the subshell still exists.
  Canceling the job only stops waiting; the build is interrupted with Ctrl+C in the terminal.
* While the job waits, the next build or compile of the project opens a new terminal instead of
  reusing the busy one: the shell reads a command line typed ahead only after the running command
  and discards it, if the command is interrupted with Ctrl+C. Verified with bash, ksh and bsh of
  the IBM i.
* A build reuses only terminals opened by builds (reuse key `build:<connection>:<directory>`), not
  terminals opened by the user. If the build terminal of the project is disconnected, e.g. after
  the connection was lost or after a restart with *Restore terminals after restart* (the reuse key
  is saved with the terminals; password and command are not), it is connected again and receives
  the command line, once the shell is ready.
* The script is kept for the next build. It is written, if it is missing or differs. The job
  deletes the marker files.
* Commands that cannot be wrapped (comment `#`, `&` or `|` at the end) run unchanged, without
  *Error List*.

### Build in a job of the RSE connection

With *Preferences > IFS Projects > Run build and compile commands > In a job of the RSE
connection*, *Build* runs without SSH login in a remote command server job of its own (copy of
the RSE system object). The SSH terminal remains for interactive work.

* The shell (`/QOpenSys/pkgs/bin/bash`, else `/QOpenSys/usr/bin/sh`) is started with
  `QP2SHELL2`. The job has no `HOME`, therefore the script exports it and runs
  `/QOpenSys/etc/profile` and `.bash_profile`/`.profile` like a login shell, so that `makei` and
  `gmake` are found. Then it runs `cd <project> && <exports> && <command>`.
* The output is appended to a log file in `<home>/.ifsprojects/tmp`, read every 500 ms and written line by line to the *Commands Log* view
  (`QSYSCommandSubSystem.logExplanation()`). `QP2SHELL2` does not return the exit code, so the
  script writes it to a file.
* Afterwards the events files of the compiled objects are shown in the *Error List* view: the
  files of `.evfevent/` of the project (TOBi) or, otherwise, the members of `EVFEVENT` of `objlib`
  or `curlib` of `iproj.json` (gmake), each changed since the start of the build.
* Builds of the same project run one after the other. *Cancel* terminates the build: the script
  writes the process ID of the shell to a file; a second job collects the shell and all its
  descendants with `ps` and terminates them with `kill -TERM` (`makei`, `make`, the `system` calls
  of the compiles, which run in jobs of their own). A compile, that is running, is aborted. TOBi may
  leave its work file `.Rules.mk.build` in the project directory.

### Syntax check

*Check Syntax* compiles an RPGLE or SQLRPGLE stream file with `OPTION(*EVENTF)` and shows the
errors of the events file in the *Error List* view of RDi. A double-click opens the source or copy
book at the error.

* The compile runs in a remote command server job of its own (copy of the RSE system object),
  set up like a TOBi build job: current directory = project directory, `setIBMiEnvCmd`, `curlib`,
  `preUsrlibl`, `postUsrlibl`, with `&NAME` resolved from the environment variables.
  `includePath` (relative to the project directory) is passed as `INCDIR`.
* RPGLE: `CRTBNDRPG ... DFTACTGRP(*NO)` or, for `NOMAIN` sources, `CRTRPGMOD`, with
  `OPTION(*NOGEN *EVENTF)`: no object is created.
* SQLRPGLE: `CRTSQLRPGI OBJTYPE(*MODULE) RPGPPOPT(*LVL2)` into `QTEMP`. `*NOGEN` would skip the
  RPG compiler, and `COMPILEOPT('OPTION(*NOGEN)')` fails with CPD0042. `RPGPPOPT(*LVL2)` makes
  host variables of copy books known to the precompiler. RNF1324 (DFTACTGRP not allowed for
  modules) is removed from the events file.
* `TGTCCSID(*JOB)` and `CVTCCSID(*JOB)` are required for UTF-8 sources (RNS9339, SQL8032).
* Unsaved changes of the file are saved first, if the user agrees; the check waits until RSE has
  uploaded the file.
* The events file is copied to `<home>/.ifsprojects/tmp`, read and deleted. Like the events files of
  RDi, its local copy is stored in project `RemoteSystemsTempFiles` (folder `IFSProjects`), one
  file per stream file, so that the table of the *Error List* is replaced when the file is checked
  again. RSE does not upload it, because it has no remote file properties. The project must not be
  hidden: the view finds new markers via resource deltas, which skip hidden resources. As in RDi,
  the view is opened before the markers are created; otherwise a new view asks whether to change
  the message style. The parser and the view of RDi (`EventsFileParser`, `QSYSErrorListViewPart`)
  exist in RDi 9.6 - 9.9.

In the LPEX editor, the syntax check is available as LPEX user action, for stream files of IFS
projects and for files of local IFS projects (see below). The plug-in does not change the LPEX
profile; it only contributes to extension point `com.ibm.lpex.preload`, so that
LPEX can load its classes. The user action is added on the preference pages of LPEX:

1. *LPEX Editor > User Actions*: name `ifsCheckSyntax`, class
   `de.tools400.ifsprojects.lpex.CheckSyntaxAction`.
2. *LPEX Editor > User Key Actions*: assign a free key to action `ifsCheckSyntax`.

The action can also be run from the LPEX command line: `action ifsCheckSyntax`.

## Local IFS projects

A local IFS project is a project of the workspace, e.g. a git working tree with `iproj.json`, that
is built on an IBM i: before each build, its files are uploaded to a deploy directory in the IFS
and the build runs there. Files are only copied from the workspace to the IFS, never back.

| Action (context menu of the project) | Function |
|---|---|
| *File > New > Project... > IFS Projects > Local IFS Project* | Creates a local IFS project: name, location (e.g. an existing git working tree) and working sets on the standard page of Eclipse, then connection, directory in the IFS, *Text files in UTF-8* (recommended, sets the encoding of the project) and the attributes of a new `iproj.json` like *New > IFS Project...* of the RSE. An existing `iproj.json` at the location is used unchanged. The creation can be undone. Shortcut in *File > New* of the perspectives Remote System Explorer and Resource (after *Reset Perspective*). |
| *Configure as IFS Project...* (projects with `iproj.json`) | Opens *Properties > IFS Project*. |
| *Properties > IFS Project* | *Build the project on an IBM i* (unchecked removes the configuration), IBM i connection (connection combo of RDi with *New...*) and directory in the IFS, default `<home>/.ifsprojects/builds/<project>`. Below, the attributes of the local `iproj.json` like the page of IFS projects (the CCSID shown is the CCSID of the uploaded file). |
| *Build on IBM i* | Saves the editors of the project, uploads the changes and runs `buildCommand` of the local `iproj.json` or the default build command in the deploy directory, like *Build* of an IFS project (terminal or job of the RSE connection). |
| *Build on IBM i* (file) | Uploads the changes of the project and runs `compileCommand` of the local `iproj.json` or the default compile command for the file, with the placeholders of IFS projects. All changes are uploaded, so that changed copy books are compiled too. |
| *Check Syntax* (RPGLE, SQLRPGLE file) | Uploads the changes of the project and checks the copy of the file in the deploy directory like the syntax check of IFS projects. The errors are shown for the local files. Also available as LPEX user action `ifsCheckSyntax` in editors of local files. |
| *Upload Changes to IFS* | Uploads new and changed files and deletes the files, that have been removed. |
| *Upload All to IFS* | Uploads all files, also unchanged files, without asking for files changed in the IFS. |
| *Properties > IFS Project > Environment* | Environment variables of the project for its connection, exported like those of IFS projects (they override the defaults of the preferences). |

* The settings are stored as persistent properties of the project in the workspace, not in
  `.project` or `.settings`, because they are specific for the user.
* Upload (IBM Toolbox for Java, copy of the RSE system object): the project is refreshed first, so
  that changes made by git are uploaded. A manifest in the state location of the plug-in
  (`deploy/<SHA-1 of connection, deploy directory and project>.manifest`) records size,
  modification time, CCSID and MD5 hash of each uploaded file. A file is uploaded again, if one of
  them has changed; if only size or time differ, the hash decides (e.g. after `git checkout`).
  Files of the manifest, that do not exist any more or are excluded now, are deleted in the deploy
  directory, also directories, that are empty then; deletes run first, because a file renamed in
  case only is the same stream file. Other files of the deploy directory, e.g. the output of TOBi,
  are never deleted.
* The deploy directory contains `.ifsprojects-deploy` with the token of the manifest. A new token is
  written before the directory is changed. If the tokens differ, e.g. because another workspace,
  Code for IBM i or someone else has changed the directory, the manifest is not trusted and all
  files are uploaded.
* Otherwise the stream files of the deploy directory are listed (one list operation per directory)
  and compared with their size and modification time after the last upload. If files have been
  changed in the IFS, the user is asked, whether they are overwritten with the local files or
  deleted; *No* cancels the upload. Missing stream files are uploaded again. Verified with myibmi:
  `listFiles()` returns size and modification time (milliseconds) like a direct access.
* The settings and environment variables (per connection) are stored as persistent properties
  (each at most 2048 characters).
* `.deployignore` or, if it does not exist, `.gitignore` of the project directory excludes files
  (git syntax: `#`, `!`, trailing `/`, leading or inner `/`, `*`, `?`, `[...]`, `**`; ignore files
  in subdirectories are not read).
* The bytes are copied unchanged; a new stream file gets the
  CCSID of the encoding of the local file (UTF-8 1208, windows-1252 1252, ISO-8859-1 819,
  ISO-8859-15 923, US-ASCII 367, otherwise 1208), a stream file with another CCSID is replaced.
  The modification time is the time of the upload, so that make rebuilds every uploaded change.
  Not uploaded: derived, team private and hidden resources, `.git`, `.project`, `.settings`,
  `.deployignore`, `.ifsprojects-deploy` and the output directories of TOBi `.logs`, `.evfevent`,
  `.deps`. Verified with myibmi: CCSID and bytes
  of new, overwritten and replaced files.
* Errors: the paths of the deploy directory in the events files are replaced by
  `/<project>/<relative path>` (`local.EventsFileMapper`; FILEID length adjusted, FILEIDCONT
  merged, case ignored, because the compilers write e.g. `/home/Jsmith`), and the errors of these
  files get the marker attribute `projectName`. Then the *Error List* opens the local file in LPEX
  (`ISeriesCompileEditManager.getOtherProjectFileEditor`, RDi 9.5 - 9.9). Other files, e.g.
  system include files, keep their IFS path.
* Limitation of RDi: the LPEX outline does not find copy books in the `includePath` directories of
  `iproj.json` (parser error `/COPY file ... not available`); compile and syntax check are not
  affected. The RPG parser of RDi searches a relative `/copy` in the current directory of the IFS
  subsystem, in the include directories of *Preferences > Program Verifiers > ILE RPG* (one global
  list), in `RPGINCDIR` of the RSE job, in the directory of the source and, for local files, in the
  directory of the source in the project and the root of referenced projects. There is no setting
  per project, that the plug-in could fill from `includePath`. According to the code of RDi 9.8
  (not tested), the outline finds `/copy inc/name.rpgleinc` of a source in the project directory.

## Files in the IFS

All files of the plug-in are stored below `<home>/.ifsprojects` of the user profile of the
connection (`HOMEDIR`, retrieved once per system and user):

* `tmp`: `ifsprojects.sh` and the temporary files `ifsprojects_<id>.*` of builds and syntax checks.
  Temporary files older than 24 hours are deleted. Earlier versions used `<home>/.eclipse/RSE`:
  their left-over temporary files are deleted, when `tmp` is created, their script, when the new
  script is written.
* `builds/<project>`: default deploy directories of local IFS projects.

## Preferences

*Preferences > IFS Projects*: SSH connection, terminals and the defaults of the IFS projects. The
preset sets the default build and compile command for gmake (`gmake`, `gmake {filename}`) or TOBi
(`makei build`, `makei compile -f {filename}`). The default environment variables apply to all
projects; a variable of a project overrides the default of the same name. *Run build and compile
commands* selects the SSH terminal (default) or a job of the RSE connection.

## Architecture

* `de.tools400.ifsprojects.subsystem` contains the subsystem *IFS Projects*. It does not extend
  the IFS subsystem of IBM, but delegates the resolution of its filter strings (`/directory/*`)
  to it. Its connector service is taken from `ToolboxConnectorServiceManager`.
* `de.tools400.ifsprojects.terminal.ITerminalLauncher` is the only interface the handlers
  know. `launch()` returns an `ITerminalCommandLine`, which tells, whether the terminal session has
  been closed since the command line was passed to it. Backends are contributed via extension point `terminalLaunchers`. The available backend
  with the highest priority is used.
* `terminal.tm` contains the backend for the TM Terminal of RDi 9.6 - 9.9: an own view that
  embeds `TerminalViewControlFactory.makeControl()`. The SSH connector is created from the
  extension registry, because the extension points and connector IDs differ between the TM
  versions. A future backend (e.g. for `org.eclipse.terminal.*`) can be added as separate bundle.
* `de.tools400.ifsprojects.syntaxcheck` contains the syntax check: `SyntaxCheckCommands` (CL
  commands, no RDi dependency), `SyntaxCheckRunner` (IBM Toolbox for Java), `ErrorList` (RDi
  parser and *Error List* view) and `SyntaxCheckJob`. `handlers.SyntaxCheckLauncher` starts it
  for the context menu and for the LPEX user action `lpex.CheckSyntaxAction`.
* `de.tools400.ifsprojects.build` contains the builds: `TerminalBuildCommands` (wrapper and
  check script, no RDi dependency) and `TerminalBuildJob` for the terminal, `RseBuildCommands`
  (shell script, no RDi dependency), `RseBuildJob`, `CommandsLog` and `LogLines` for the job of the
  RSE connection, `BuildEvents` and `BuildErrors` for both. `handlers.TerminalBuildLauncher` and
  `handlers.RseBuildLauncher` start them. `remote.RemoteJobs` holds the IBM Toolbox helpers of
  syntax check and build and the paths of the files in the IFS.
* `project.propertypages`: `AbstractIProjPropertyPage` (attributes of `iproj.json`) and
  `AbstractEnvironmentPropertyPage` are shared by the pages of IFS projects and local IFS projects,
  `LocalProjectSettingsGroup` (build on IBM i, connection, deploy directory) by the property page
  and the wizard of local IFS projects. `project.wizards.IProjDefaultsGroup` (new `iproj.json`) is
  shared by the wizards of IFS projects and local IFS projects.
* `de.tools400.ifsprojects.local` contains the local IFS projects: `LocalProject` (settings and
  `iproj.json`), `Deployment` (upload), `DeployManifest`, `IgnoreRules`, `SourceMapping` and
  `EventsFileMapper` (errors of the local files). The builds receive the `SourceMapping` through the launchers and pass it to
  `BuildErrors`.

## Development

* Workspace: the repository is cloned into the RDi workspace. The target platform is the running
  platform (RDi).
* Java 8 byte code, so that the plug-in runs in RDi 9.6.
* Launch configurations: `eclipse/IFS Projects Core Plugin/launches`.
* Unit tests: fragment `eclipse/IFS Projects JUnit` (run as JUnit Test).
* Update site: open `eclipse/IFS Projects Update Site/site.xml` and use *Build All*.
* Release: `build/IFS Projects Build/build/build.xml`, the same script as in iSphere, iRPGUnit
  and JCRCMDS; project specific settings are in `build.properties`. The update site and the home
  page are written to `docs/` of repository `ifs-terminal` (GitHub Pages).

## License

Common Public License v1.0, see http://www.eclipse.org/legal/cpl-v10.html.
The icons of the terminal view are taken from the Eclipse TM Terminal (EPL).
