/*
 * Copyright (C) 2011 by Daniel Anderson
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package dan.tools.swcv;

import java.io.Reader;
import java.util.Hashtable;
import java.util.Vector;

public class PackageOrder {
    private Hashtable<String, Integer> pacakageNameToRank;
    private Vector<Vector<String>> rankToPackageNames;

    public PackageOrder(Reader reader) throws Exception {
        pacakageNameToRank = new Hashtable<String, Integer>();
        rankToPackageNames = new Vector<Vector<String>>();
        rankToPackageNames.add(new Vector<String>());

        StringBuffer buf = new StringBuffer();
        int c;
        boolean maintainRank = false;

        while ((c = reader.read()) != -1) {
            char ch = (char) c;

            if (ch == ',') {
                if (buf.length() > 0) {
                    addPackage(buf.toString());
                    buf.setLength(0);
                } else {
                    throw new Exception(
                            "package order parse error: Too many commas (',') ?");
                }

                if (!maintainRank) {
                    rankToPackageNames.add(new Vector<String>());
                }
            } else if (ch == '(') {
                maintainRank = true;
            } else if (ch == ')') {
                maintainRank = false;
            } else if (!Character.isWhitespace(ch)) {
                buf.append(ch);
            }
        }

        if (buf.length() > 0) {
            addPackage(buf.toString());
        }

        if (maintainRank != false) {
            throw new Exception(
                    "package order parse error: parentheses mismatch");
        }
    }

    public int getRank(String pkg) {
        Integer rank = pacakageNameToRank.get(pkg);
        return (rank != null) ? rank.intValue() : -1;
    }

    public Iterable<String> pacakgesIterator(int rank) {
        if (rank >= 0 && rank < rankToPackageNames.size()) {
            return rankToPackageNames.get(rank);
        } else {
            return new Vector<String>();
        }
    }

    private void addPackage(String packageName) {
        int rank = rankToPackageNames.size() - 1;

        pacakageNameToRank.put(packageName, new Integer(rank));

        Vector<String> pkgs = rankToPackageNames.get(rank);

        if (!pkgs.contains(packageName)) {
            pkgs.add(packageName);
        }
    }
}
