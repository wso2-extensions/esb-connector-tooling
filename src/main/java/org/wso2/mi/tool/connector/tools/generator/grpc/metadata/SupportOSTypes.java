/*
 *  Copyright (c) 2025, WSO2 LLC. (https://www.wso2.com).
 *
 *  WSO2 LLC. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package org.wso2.mi.tool.connector.tools.generator.grpc.metadata;

import java.util.HashMap;
import java.util.Map;

/**
 * OS types which is supported by this tool.
 */
class SupportOSTypes {

    static final String AIX = "aix";
    static final String HPUX = "hpux";
    static final String OS400 = "os400";
    static final String LINUX = "linux";
    static final String MACOSX = "macosx";
    static final String OSX = "osx";
    static final String FREEBSD = "freebsd";
    static final String OPENBSD = "openbsd";
    static final String NETBSD = "netbsd";
    static final String WINDOWS = "windows";
    static final String SOLARIS = "solaris";
    static final String SUNOS = "sunos";
    // Map of OS name prefixes to their normalized values
    static final Map<String, String> OSMAP;

    static {
        OSMAP = new HashMap<>();
        OSMAP.put(AIX, AIX);
        OSMAP.put(HPUX, HPUX);
        OSMAP.put(LINUX, LINUX);
        OSMAP.put(MACOSX, OSX);
        OSMAP.put(OSX, OSX);
        OSMAP.put(FREEBSD, FREEBSD);
        OSMAP.put(OPENBSD, OPENBSD);
        OSMAP.put(NETBSD, NETBSD);
        OSMAP.put(SOLARIS, SUNOS);
        OSMAP.put(SUNOS, SUNOS);
        OSMAP.put(WINDOWS, WINDOWS);
    }
    private SupportOSTypes() {
    }

}
