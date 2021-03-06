/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package org.sonar.server.test.ws;

import org.junit.Before;
import org.junit.Test;
import org.sonar.api.server.ws.RailsHandler;
import org.sonar.api.server.ws.WebService;
import org.sonar.server.ws.WsTester;

import static org.fest.assertions.Assertions.assertThat;

public class TestsWsTest {

  WebService.Controller controller;

  @Before
  public void setUp() throws Exception {
    WsTester tester = new WsTester(new TestsWs());
    controller = tester.controller("api/tests");
  }

  @Test
  public void define_controller() throws Exception {
    assertThat(controller).isNotNull();
    assertThat(controller.since()).isEqualTo("3.5");
    assertThat(controller.description()).isNotEmpty();
    assertThat(controller.actions()).hasSize(2);
  }

  @Test
  public void define_plan_action() throws Exception {
    WebService.Action action = controller.action("plan");
    assertThat(action).isNotNull();
    assertThat(action.isInternal()).isTrue();
    assertThat(action.isPost()).isFalse();
    assertThat(action.handler()).isInstanceOf(RailsHandler.class);
    assertThat(action.responseExampleAsString()).isNotEmpty();
    assertThat(action.params()).hasSize(2);
  }

  @Test
  public void define_testable_action() throws Exception {
    WebService.Action action = controller.action("testable");
    assertThat(action).isNotNull();
    assertThat(action.isInternal()).isTrue();
    assertThat(action.isPost()).isFalse();
    assertThat(action.handler()).isInstanceOf(RailsHandler.class);
    assertThat(action.responseExampleAsString()).isNotEmpty();
    assertThat(action.params()).hasSize(2);
  }

}
