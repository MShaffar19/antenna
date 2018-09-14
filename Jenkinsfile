/*
 * Copyright (c) Bosch Software Innovations GmbH 2018.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 */

pipeline {
    agent any

    stages {
        stage('Build') {
            steps {
                withMaven() {
                    sh 'mvn -B -DskipTests package'
                }
            }
        }
        
        stage('Test') {
            steps {
                withMaven() {
                    sh 'mvn test'
                }
            }
        }

        stage('Install') {
            steps {
                withMaven() {
                    sh 'mvn -B -DskipTests install'
                }
            }
        }
    }
}
