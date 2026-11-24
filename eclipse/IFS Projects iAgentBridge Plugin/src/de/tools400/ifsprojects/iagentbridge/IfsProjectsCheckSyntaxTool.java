/*******************************************************************************
 * Copyright (c) 2026 Thomas Raddatz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package de.tools400.ifsprojects.iagentbridge;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IProgressMonitor;

import de.tools400.ifsprojects.api.CompileException;
import de.tools400.ifsprojects.api.CompileResult;
import de.tools400.ifsprojects.api.FileCompiler;

/**
 * Tool 'ifs_projects_check_syntax': checks the syntax of an RPGLE or SQLRPGLE
 * file of a local IFS project on the IBM i, like 'Check Syntax', see
 * {@link FileCompiler#checkSyntax(IFile, IProgressMonitor)}.
 */
public class IfsProjectsCheckSyntaxTool extends AbstractFileCompilerTool {

    private static final String DESCRIPTION = "Checks the syntax of an RPGLE or SQLRPGLE source file of a local IFS project of RDi " //$NON-NLS-1$
        + "(Eclipse) on the IBM i, like 'Check Syntax' of the plug-in IFS Projects: uploads the changes of the project to its deploy " //$NON-NLS-1$
        + "directory in the IFS and compiles the file there in QTEMP of a job of its own with the library list and the include " //$NON-NLS-1$
        + "directories of iproj.json, without creating an object (an SQLRPGLE source without errors leaves a module in QTEMP of that " //$NON-NLS-1$
        + "job). Returns the command, 'success' (no message with a severity above 10), the highest severity, the number of messages " //$NON-NLS-1$
        + "per severity and the messages of the compilers with file, line, column, message ID, severity and text, sorted by severity, " //$NON-NLS-1$
        + "file and line; the lines of an SQLRPGLE source are those of the source, not of the precompiled one. Use it after you " //$NON-NLS-1$
        + "changed an RPG source, to check that it compiles; ifs_projects_compile also creates the objects. " //$NON-NLS-1$
        + CONDITIONS;

    @Override
    public String getName() {
        return "ifs_projects_check_syntax"; //$NON-NLS-1$
    }

    @Override
    public String getDescription() {
        return DESCRIPTION;
    }

    @Override
    protected String getJobName(IFile aFile) {
        return Messages.bind(Messages.Task_Checking_syntax_of_A, aFile.getName());
    }

    @Override
    protected CompileResult run(IFile aFile, IProgressMonitor aMonitor) throws CompileException, InterruptedException {
        return FileCompiler.checkSyntax(aFile, aMonitor);
    }

    @Override
    protected CompileReport createReport(String aFile, CompileResult aResult, int aMinSeverity, int aMaxMessages) {
        return CompileReport.forSyntaxCheck(aFile, aResult.getCommand(), aMinSeverity, aMaxMessages);
    }
}
