/* The contents of this file are subject to the terms
* of the Common Development and Distribution License
* (the License). You may not use this file except in
* compliance with the License.
*
* You can obtain a copy of the License at
* http://www.sun.com/cddl/cddl.html or
* install_dir/legal/LICENSE
* See the License for the specific language governing
* permission and limitations under the License.
*
* When distributing Covered Code, include this CDDL
* Header Notice in each file and include the License file
* at install_dir/legal/LICENSE.
* If applicable, add the following below the CDDL Header,
* with the fields enclosed by brackets [] replaced by
* your own identifying information:
* "Portions Copyrighted [year] [name of copyright owner]"
*
* Copyright 2005 Sun Microsystems Inc. All Rights Reserved
*/

package com.sun.faban.harness.webclient;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.StringTokenizer;

/**
 *
 * @author Sheetal Patil
 */
public class TagEngine implements Serializable{

    HashMap<String, Entry> tagEntries = new HashMap<String, Entry>();
    HashMap<String, HashSet<Entry>> runEntries = new HashMap<String, HashSet<Entry>>();

    /**
     * Searches the tag engine for runs matching the given tags.
     * @param tags The tag in question, '/' seperated from sub-tags
     * @return The set of run ids matching the given tags
     */
    public Set<String> search(String tags) {
        String[] tagsArray = null;
        if (!tags.equals("")) {
            StringTokenizer tok = new StringTokenizer(tags, " ,:;");
            tagsArray = new String[tok.countTokens()];
            int count = tok.countTokens();
            int i = 0;
            while (i < count) {
                String nextT = tok.nextToken().trim();
                tagsArray[i] = nextT;
                i++;
            }
        }

        HashSet<String> finalAnswer = new HashSet<String>();
        int count = 0;
        for (String tag : tagsArray){
            HashSet<String> answer = new HashSet<String>();
            Entry entry = findEntry(tag);
            if (entry != null) {

                // Flatten the entry hierarchy into the ArrayList entries.
                ArrayList<Entry> entries = new ArrayList<Entry>();
                entries.addAll(entry.subtags.values());

                // Keep flattening down the hierarchy
                // entries.size keeps increasing as we go down
                // the hierarchy. That's why we need to keep
                // evaluating entries.size() and cannot use an iterator.
                for (int i = 0; i < entries.size(); i++) {
                    Entry subEntry = entries.get(i);
                    entries.addAll(subEntry.subtags.values());
                }

                // Adds run ids from the flattened entries to the answer.
                answer.addAll(entry.runIds);
                for (Entry subEntry : entries) {
                    answer.addAll(subEntry.runIds);
                }
                if(count == 0){
                    finalAnswer = answer;
                }
            }
            finalAnswer.retainAll(answer);
            answer = null;
            count++;
        }
        return finalAnswer;
    }
    
    private Entry findEntry(String tag) {
        tag = tag.toLowerCase();
        StringTokenizer t = new StringTokenizer(tag, "/");
        String topTag = t.nextToken();
        Entry entry = tagEntries.get(topTag);
        String subTag = topTag;
        // Keep finding the entry that represents the exact tag/subtag.
        while(entry != null && t.hasMoreTokens()) {
            subTag = subTag + "/" + t.nextToken();
            entry = entry.subtags.get(subTag);
            if (entry == null)
                break;
        }
        return entry;
    }

    private void removeEntry(Entry entry) {
        entry.subtags.remove(entry);
    }

    public void removeRunId(String runId) {
        HashSet<Entry> removeSet = runEntries.get(runId);
        for (Entry entry : removeSet) {
            entry.runIds.remove(runId);
        }
        runEntries.remove(runId);
    }

    private String[] getTagsArray(String tag) {
        String[] tagsArray = null;
        if (!tag.equals("")) {
            StringTokenizer tok = new StringTokenizer(tag, "/");
            tagsArray = new String[tok.countTokens()];
            int count = tok.countTokens();
            int i = 0;
            while (i < count) {
                String nextT = tok.nextToken().trim();
                if (i == 0) {
                    tagsArray[i] = nextT;
                } else {
                    tagsArray[i] = tagsArray[i - 1] + "/" + nextT;
                }
                i++;
            }
        }
        return tagsArray;
    }

    /**
     * Adds a set of tags for a run id to the tag engine.
     * Old tags for the run id will be replaced with the new one.
     * If tags is null or an empty array, all tags will be removed for the
     * given run id.
     * @param runId The runId
     * @param tags The list of tags to add
     */
    public void add(String runId, String[] tags) {
        HashSet<Entry> entrySet = runEntries.get(runId);
        if(entrySet == null)
            entrySet = new HashSet<Entry>();
        HashSet<Entry> newEntrySet = new HashSet<Entry>();
        for (String tag : tags) {
            tag = tag.toLowerCase();
            String[] tagsArray = getTagsArray(tag);
            Entry entry = null;
            int count = 0;
            for(String subtag : tagsArray){
                if (count == 0) {
                    entry = findEntry(subtag);
                    if (entry == null) {
                        entry = new Entry();
                        entry.fullTagName = subtag;
                        tagEntries.put(subtag, entry);
                    }
                }else{
                    Entry subEntry = findEntry(subtag);
                    if (subEntry == null) {
                          subEntry  = new Entry();
                        subEntry.fullTagName = subtag;
                        tagEntries.put(subtag, subEntry);
                        entry.subtags.put(subtag, subEntry);
                    }
                }
                entry = tagEntries.get(subtag);
                count++;
            }
            entry.runIds.add(runId);
            tagEntries.put(tag, entry);
            newEntrySet.add(entry);
        }

        HashSet<Entry> removeEntrySet = new HashSet<Entry>(entrySet);
        removeEntrySet.removeAll(newEntrySet);

        for (Entry entry : removeEntrySet) {
            entry.runIds.remove(runId);
            if (entry.runIds.size() == 0 && entry.subtags.size() == 0) {
                removeEntry(entry);
            }
        }
        entrySet.removeAll(removeEntrySet);
        entrySet.addAll(newEntrySet);
        runEntries.put(runId, entrySet);
    }

    static class Entry implements Serializable{
        String fullTagName;
        HashSet<String> runIds = new HashSet<String>();
        HashMap<String, Entry> subtags = new HashMap<String, Entry>();
    }
}