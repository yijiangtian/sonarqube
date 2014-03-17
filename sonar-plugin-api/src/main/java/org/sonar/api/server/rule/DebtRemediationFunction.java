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

package org.sonar.api.server.rule;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

/**
 * @since 4.3
 */
public class DebtRemediationFunction {

  public static enum Type {
    LINEAR, LINEAR_OFFSET, CONSTANT_ISSUE
  }

  public static class ValidationException extends RuntimeException {

    public ValidationException(String message) {
      super(message);
    }
  }

  private Type type;
  private String factor;
  private String offset;

  private DebtRemediationFunction(Type type, @Nullable String factor, @Nullable String offset) {
    this.type = type;
    // TODO validate factor and offset format
    this.factor = StringUtils.deleteWhitespace(factor);
    this.offset = StringUtils.deleteWhitespace(offset);
    validate();
  }

  private void validate(){
    switch (type) {
      case LINEAR:
        if (this.factor == null || this.offset != null) {
          throw new ValidationException(String.format("%s is invalid, Linear remediation function should only define a factor", this));
        }
        break;
      case LINEAR_OFFSET:
        if (this.factor == null || this.offset == null) {
          throw new ValidationException(String.format("%s is invalid,  Linear with offset remediation function should define both factor and offset", this));
        }
        break;
      case CONSTANT_ISSUE:
        if (this.factor != null || this.offset == null) {
          throw new ValidationException(String.format("%s is invalid, Constant/issue remediation function should only define an offset", this));
        }
        break;
      default:
        throw new IllegalStateException(String.format("Remediation function of %s is unknown", this));
    }
  }

  public static DebtRemediationFunction create(Type type, @Nullable String factor, @Nullable String offset) {
    return new DebtRemediationFunction(type, factor, offset);
  }

  public static DebtRemediationFunction createLinear(String factor) {
    return new DebtRemediationFunction(Type.LINEAR, factor, null);
  }

  public static DebtRemediationFunction createLinearWithOffset(String factor, String offset) {
    return new DebtRemediationFunction(Type.LINEAR_OFFSET, factor, offset);
  }

  public static DebtRemediationFunction createConstantPerIssue(String offset) {
    return new DebtRemediationFunction(Type.CONSTANT_ISSUE, null, offset);
  }

  public Type type() {
    return type;
  }

  @CheckForNull
  public String factor() {
    return factor;
  }

  @CheckForNull
  public String offset() {
    return offset;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    DebtRemediationFunction that = (DebtRemediationFunction) o;

    if (type != that.type) {
      return false;
    }
    if (factor != null ? !factor.equals(that.factor) : that.factor != null) {
      return false;
    }
    if (offset != null ? !offset.equals(that.offset) : that.offset != null) {
      return false;
    }

    return true;
  }

  @Override
  public int hashCode() {
    int result = type.hashCode();
    result = 31 * result + (factor != null ? factor.hashCode() : 0);
    result = 31 * result + (offset != null ? offset.hashCode() : 0);
    return result;
  }

  @Override
  public String toString() {
    return new ReflectionToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE).toString();
  }
}