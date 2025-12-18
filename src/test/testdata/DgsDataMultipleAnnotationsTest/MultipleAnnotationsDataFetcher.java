/*
 * Copyright 2021 Netflix, Inc.
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

import com.netflix.graphql.dgs.DgsComponent;
import com.netflix.graphql.dgs.DgsData;

@DgsComponent
public class MultipleAnnotationsDataFetcher {

    // Using implicit @Repeatable - multiple @DgsData annotations
    // These should each link to their specific type's field in the schema
    @DgsData(parentType = "Movie", field = "title")
    @DgsData(parentType = "Show", field = "title")
    public String title() {
        return "Sample Title";
    }

    // Using implicit @Repeatable with four types
    @DgsData(parentType = "Movie", field = "contentAdvisory")
    @DgsData(parentType = "Show", field = "contentAdvisory")
    @DgsData(parentType = "Season", field = "contentAdvisory")
    @DgsData(parentType = "Episode", field = "contentAdvisory")
    public Advisory contentAdvisory() {
        return new Advisory();
    }

    static class Advisory {
        public String rating;
    }
}