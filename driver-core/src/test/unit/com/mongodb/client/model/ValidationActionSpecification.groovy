/*
 * Copyright 2015 MongoDB, Inc.
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

package com.mongodb.client.model

import spock.lang.Specification

class ValidationActionSpecification extends Specification {

    def 'should return the expected string value'() {
        expect:
        validationAction.getValue() == expectedString

        where:
        validationAction          | expectedString
        ValidationAction.WARN     | 'warn'
        ValidationAction.ERROR    | 'error'
    }

    def 'should support valid string representations'() {
        expect:
        ValidationAction.fromString(actionString) instanceof ValidationAction

        where:
        actionString << ['error', 'warn', 'ERROR', 'WARN']
    }

    def 'should throw an illegal Argument exception for invalid values'() {
        when:
        ValidationAction.fromString(actionString)

        then:
        thrown(IllegalArgumentException)

        where:
        actionString << [null, 'info']
    }
}
