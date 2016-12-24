/**
 * Copyright 2004-2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.ibatis.common.logging.jdk14;

import java.util.logging.Logger;
import java.util.logging.Level;

public class Jdk14LoggingImpl implements com.ibatis.common.logging.Log {

  private Logger log;

  public Jdk14LoggingImpl(Class clazz) {
    log = Logger.getLogger(clazz.getName());
  }

  public boolean isDebugEnabled() {
    return log.isLoggable(Level.FINE);
  }

  public void error(String s, Throwable e) {
    log.log(Level.SEVERE, s, e);
  }

  public void error(String s) {
    log.log(Level.SEVERE, s);
  }

  public void debug(String s) {
    log.log(Level.FINE, s);
  }

  public void warn(String s) {
    log.log(Level.WARNING, s);
  }

}
