/*******************************************************************************
 * Copyright (c) 2026 Thomas Raddatz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package de.tools400.ifsprojects.iagentbridge;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.ibm.etools.iseries.comm.interfaces.ISequentialFileReader;
import com.ibm.etools.iseries.util.evfparser.EventsFileParserCore;
import com.ibm.etools.iseries.util.evfparser.IMarkerCreator;
import com.ibm.etools.iseries.util.evfparser.QSYSEventsFileErrorInformationRecord;
import com.ibm.etools.iseries.util.evfparser.QSYSEventsFileExpansionProcessorCore;

/**
 * Reads the messages of the compilers from the content of events files
 * (EVFEVENT) with the parser of RDi, <code>EventsFileParserCore</code> of the
 * bundle <code>com.ibm.etools.iseries.toolbox</code>, which the 'Error List'
 * view uses, too.
 * <p>
 * The parser maps the lines of the expanded source of a precompiler back to the
 * lines of the source and its copy books: the RPG compiler reports the errors
 * of an SQLRPGLE source for a temporary member of QTEMP, which the SQL
 * precompiler has created.
 * <p>
 * The class has no Eclipse dependencies, so that it can be tested outside of
 * RDi with the jars of the bundle on the class path.
 */
public final class EventsFileMessages {

    private EventsFileMessages() {
        // Utility class
    }

    /**
     * Returns the messages of events files.
     *
     * @param anEvents - content of the events files, one after the other, or
     *        <code>null</code>
     * @param aCcsid - CCSID of the job, in which the events were read; the
     *        lengths of the texts are counted in bytes of this CCSID
     * @param aLocalFiles - paths of the events, that are replaced, and their
     *        replacements, e.g. the absolute paths of the local files
     * @return messages in the order of the events files
     * @throws IOException if the events could not be parsed
     */
    public static List<CompileReport.Message> parse(String anEvents, int aCcsid, Map<String, String> aLocalFiles) throws IOException {

        if (anEvents == null) {
            return Collections.emptyList();
        }

        final BufferedReader reader = new BufferedReader(new StringReader(anEvents));
        EventsFileParserCore parser = new EventsFileParserCore();
        parser.setProcessor(new QSYSEventsFileExpansionProcessorCore());
        try {
            parser.parse(new ISequentialFileReader() {
                public String readNextLine() throws IOException {
                    return reader.readLine();
                }
            }, aCcsid, new IMarkerCreator() {
                public void createMarker(QSYSEventsFileErrorInformationRecord aRecord, String aLocation, String aReadOnly) {
                    // The errors are read with getAllErrors(), which maps the
                    // lines of an expanded source back.
                }

                public void updateConnectionName(String aConnectionName, int aCount) {
                    // Not needed
                }
            });
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException("The events file could not be parsed: " + e.getLocalizedMessage(), e); //$NON-NLS-1$
        }

        List<CompileReport.Message> messages = new ArrayList<>();
        for (Object error : parser.getAllErrors()) {
            messages.add(toMessage((QSYSEventsFileErrorInformationRecord)error, aLocalFiles));
        }

        return messages;
    }

    private static CompileReport.Message toMessage(QSYSEventsFileErrorInformationRecord aRecord, Map<String, String> aLocalFiles) {

        String file = aRecord.getFileName();
        String localFile = aLocalFiles.get(file);
        if (localFile != null) {
            file = localFile;
        }

        return new CompileReport.Message(file, toInt(aRecord.getStartErrLine()), toInt(aRecord.getTokenStart()), aRecord.getMsgId(),
            toInt(aRecord.getSevNum()), aRecord.getMsg() == null ? "" : aRecord.getMsg().trim()); //$NON-NLS-1$
    }

    private static int toInt(String aValue) {

        if (aValue == null) {
            return 0;
        }

        try {
            return Integer.parseInt(aValue.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
