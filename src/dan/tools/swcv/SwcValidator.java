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

import java.io.FileReader;
import java.io.PrintStream;
import java.io.Reader;
import java.io.StringReader;
import java.util.Vector;

/**
 * Entry point for SwcValidator, a program that determines whether or not an
 * Actionscript 3 SWC meets a user defined package dependency order specification.
 * 
 * Programmer notes:
 * 
 * SWC reading and parsing is handled in Swc.java.
 * 
 * The package dependency order specification is read and parsed by PackageOrder.java.
 * 
 * The algorithms that print the package report and validate a SWC are in this class. 
 */
public class SwcValidator {
    public static void main(String[] args) {
        Options options = new Options(args);
        int result;

        if (options.isValid()) {
            result = swcv(options);
        } else {
            usage("Invalid arguments.");
            result = 1;
        }

        System.exit(result);
    }

    private static int swcv(Options options) {
        Swc swc;

        // Parse the swc. All modes require the swc package information.
        try {
            swc = new Swc(options.getSwcFilename());
        } catch (Exception e) {
            System.out.println("Error opening swc: " + e.getMessage());
            return 1;
        }

        if (Options.MODE_PKG_REPORT.equals(options.getMode())) {
            return packageReport(swc);
        } else {
            PackageOrder packageOrder = null;
            Reader reader = null;

            // Choose a reader.
            if (Options.MODE_PKG_ORDER_FILE.equals(options.getMode())) {
                try {
                    reader = new FileReader(options.getPackageOrderData());
                } catch (Exception e) {
                    System.out.println("Error opening package order file: "
                            + e.getMessage());
                    return 1;
                }
            } else if (Options.MODE_PKG_ORDER.equals(options.getMode())) {
                reader = new StringReader(options.getPackageOrderData());
            }

            // Parse the package order data.
            try {
                packageOrder = new PackageOrder(reader);
            } catch (Exception e) {
                System.out.println("Error parsing package order data: "
                        + e.getMessage());
                return 1;
            } finally {
                try {
                    reader.close();
                } catch (Exception e) {
                    // ignore
                }
            }

            // Now, validate the swc.
            return validatePackageOrder(swc, packageOrder);
        }
    }

    private static void usage(String msg) {
        PrintStream o = System.out;
        
        if (msg != null) {
            o.println(msg);
        }

        o.println("Usage: swcv <swcfile> [options]");
        
        o.println("Options:");
        o.println("--pkg-order-file <package-order-file>");
        o.println("--pkg-order package-order");
        o.println("--pkg-report");
        
        o.println("Package Dependency Order:");
        
        // dotted package names
        // all packages available in swc must be 
        //   defined in pdo.
        // only package names that are in the swc
        // whitespace ok
        // A, B, C
        // W, (X, Y), Z
        
//        System.out.println("Validate swc package depedencies with " +
//        		"package order defined in a file.");
//        System.out.println("\tjava -jar SwcValidator.jar " +
//                "<swcfile> --pkg-order-file <package-order-file>");
//
//        System.out.println("Validate swc package depedencies with " +
//        		"package order.");
//        System.out.println("\tjava -jar SwcValidator.jar " +
//        		"<swcfile> --pkg-order package-order-string");
//
//        System.out.println("Print a package report for a swc.");
//        System.out.println("\tjava -jar SwcValidator.jar " +
//        		"<swcfile> --pkg-report");
    }

    private static int packageReport(Swc swc) {
        for (Package pkg : swc.packagesIterator()) {
            Vector<Symbol> pkgDep = new Vector<Symbol>();
            Vector<String> pkgDepByPackage = new Vector<String>();
            Vector<Symbol> extDep = new Vector<Symbol>();
            Vector<String> extDepByPackage = new Vector<String>();

            for (Symbol dep : pkg.dependenciesIterator()) {
                Package p = swc.getPackage(dep.getPackageName());

                if (p != null && p.hasExport(dep)) {
                    pkgDep.add(dep);

                    if (!pkgDepByPackage.contains(dep.getPackageName())) {
                        pkgDepByPackage.add(dep.getPackageName());
                    }
                } else {
                    extDep.add(dep);

                    if (!extDepByPackage.contains(dep.getPackageName())) {
                        extDepByPackage.add(dep.getPackageName());
                    }
                }
            }

            System.out.println(pkg.getName());

            System.out.println("\tExports");

            for (Symbol exp : pkg.exportsIterator()) {
                System.out.println("\t\t" + exp.getName());
            }

            System.out.println("\tDependencies");

            if (!pkgDep.isEmpty()) {
                for (Symbol s : pkgDep) {
                    System.out.println("\t\t" + s);
                }
            } else {
                System.out.println("\t\t<Empty>");
            }

            System.out.println("\tDependencies By Package");

            if (!pkgDepByPackage.isEmpty()) {
                for (String p : pkgDepByPackage) {
                    System.out.println("\t\t" + p);
                }
            } else {
                System.out.println("\t\t<Empty>");
            }

            System.out.println("\tExternal Dependencies");

            if (!extDep.isEmpty()) {
                for (Symbol s : extDep) {
                    System.out.println("\t\t" + s);
                }
            } else {
                System.out.println("\t\t<Empty>");
            }

            System.out.println("\tExternal Dependencies By Package");

            if (!extDepByPackage.isEmpty()) {
                for (String p : extDepByPackage) {
                    System.out.println("\t\t" + p);
                }
            } else {
                System.out.println("\t\t<Empty>");
            }
        }

        return 0;
    }

    private static int validatePackageOrder(Swc swc, PackageOrder dependencies) {
        // Ensure that the PackageOrder ranks all packages defined in the
        // swc. If not, raise an error.
        for (Package pkg : swc.packagesIterator()) {
            if (dependencies.getRank(pkg.getName()) == -1) {
                System.out
                        .println("Package order file should rank ALL packages in swc. Package "
                                + pkg.getName() + " is missing.");
                return 1;
            }
        }

        // Algorithm:
        //
        // For each package in the swc, get it's package rank from the
        // package order that the user specified. The check above ensures
        // that each package in the swc has a specified rank.
        //
        // Packages of lower rank cannot depend on symbols from packages
        // of higher rank. Each package has a list of dependent symbols,
        // so this check can be performed easily. Note: Dependencies on
        // external symbols (symbols not defined in the swc) will be
        // ignored.
        //
        // If all packages in the swc pass the above tests, the swc's
        // packages meet the package dependency order specification.
        for (Package pkg : swc.packagesIterator()) {
            int pkgRank = dependencies.getRank(pkg.getName());

            for (Symbol dep : pkg.dependenciesIterator()) {
                Package p = swc.getPackage(dep.getPackageName());

                if (p == null || !p.hasExport(dep)) {
                    continue;
                }

                if (dependencies.getRank(dep.getPackageName()) > pkgRank) {
                    System.out
                            .println("Dependency validation failure: package "
                                    + pkg.getName()
                                    + " cannot import or use symbol: " + dep);
                    return 1;
                }
            }
        }

        return 0;
    }

    /**
     * Parses, validates and holds the command line arguments to the
     * SwcValidator program.
     */
    private static class Options {
        private static final String MODE_PKG_REPORT = "--pkg-report";
        private static final String MODE_PKG_ORDER_FILE = "--pkg-order-file";
        private static final String MODE_PKG_ORDER = "--pkg-order";

        private String mode;
        private String swcFilename;
        private String packageOrderData;

        public Options(String[] args) {
            int i = 0;

            while (i < args.length) {
                if (MODE_PKG_REPORT.equals(args[i])) {
                    mode = MODE_PKG_REPORT;
                } else if (MODE_PKG_ORDER_FILE.equals(args[i])) {
                    mode = MODE_PKG_ORDER_FILE;
                    i++;
                    packageOrderData = (i < args.length) ? args[i] : null;
                } else if (MODE_PKG_ORDER.equals(args[i])) {
                    mode = MODE_PKG_ORDER;
                    i++;
                    packageOrderData = (i < args.length) ? args[i] : null;
                } else {
                    if (swcFilename != null) {
                        return;
                    }

                    swcFilename = args[i];
                }

                i++;
            }
        }

        public String getMode() {
            return mode;
        }

        public String getSwcFilename() {
            return swcFilename;
        }

        public String getPackageOrderData() {
            return packageOrderData;
        }

        public boolean isValid() {
            if (swcFilename == null) {
                return false;
            } else if (mode == null) {
                return false;
            } else if (mode.equals(MODE_PKG_ORDER)
                    || mode.equals(MODE_PKG_ORDER_FILE)) {
                if (packageOrderData == null) {
                    return false;
                }
            }

            return true;
        }
    }
}
