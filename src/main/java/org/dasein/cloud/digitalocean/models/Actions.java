/**
 * Copyright (C) 2012-2015 Dell, Inc.
 * See annotations for authorship information
 *
 * ====================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ====================================================================
 */

package org.dasein.cloud.digitalocean.models;

import org.dasein.cloud.digitalocean.models.rest.DigitalOceanRestModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class Actions implements DigitalOceanRestModel {

    private List<Action> actions;

    public void addAction(Action r) {
        getActions().add(r);
    }

    public List<Action> getActions() {
        if( actions == null ) {
            actions = new ArrayList<Action>();
        }
        return actions;
    }
}
