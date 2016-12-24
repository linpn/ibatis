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
package com.ibatis.common.beans;

import junit.framework.TestCase;

public class ComplexBeanProbeTest extends TestCase {

  public void testSetObject() {
    SimpleClass mySimpleClass = new SimpleClass();
    Probe probe = ProbeFactory.getProbe(mySimpleClass);
    probe.setObject(mySimpleClass, "myInt", Integer.valueOf(1));
    assertEquals(1, mySimpleClass.getMyInt());
    probe.setObject(mySimpleClass, "myInt", Integer.valueOf(2));
    assertEquals(2, mySimpleClass.getMyInt());
    try {
      probe.setObject(mySimpleClass, "myInt", null);
      fail();
    } catch (RuntimeException e) {
      assertTrue(e.getMessage().contains("'myInt' to value 'null'"));
    }
    try {
      probe.setObject(mySimpleClass, "myInt", Float.valueOf(1.2f));
      fail();
    } catch (RuntimeException e) {
      assertTrue(e.getMessage().contains("'myInt' to value '1.2'"));
    }
  }

  public class SimpleClass {

    int myInt;

    public int getMyInt() {
      return myInt;
    }

    public void setMyInt(int myInt) {
      this.myInt = myInt;
    }
  }

}
