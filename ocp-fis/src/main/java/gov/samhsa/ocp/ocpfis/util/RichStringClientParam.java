package gov.samhsa.ocp.ocpfis.util;

/*
 * #%L
 * HAPI FHIR - Core Library
 * %%
 * Copyright (C) 2014 - 2017 University Health Network
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import ca.uhn.fhir.model.primitive.StringDt;
import ca.uhn.fhir.rest.api.Constants;
import ca.uhn.fhir.rest.gclient.ICriterion;
import ca.uhn.fhir.rest.gclient.StringClientParam;

import java.util.Arrays;
import java.util.List;

public class RichStringClientParam extends StringClientParam{

	private final String myParamName;

	public RichStringClientParam(String theParamName) {
        super(theParamName);
        myParamName = theParamName;
	}

	/**
	 * The string contains the given value (servers will often, but are not required to) implement this as a contains match,
	 * meaning that a value of "mi" would match "smi" and "smith".
	 */
	public IStringMatch contains() {
		return new StringContains();
	}

	private class StringContains implements IStringMatch {
		@Override
		public ICriterion<StringClientParam> value(String theValue) {
			return new StringCriterion<StringClientParam>(getParamName() + Constants.PARAMQUALIFIER_STRING_CONTAINS, theValue);
		}

		@Override
		public ICriterion<StringClientParam> value(StringDt theValue) {
			return new StringCriterion<StringClientParam>(getParamName() + Constants.PARAMQUALIFIER_STRING_CONTAINS, theValue.getValue());
		}

		@Override
		public ICriterion<StringClientParam> values(List<String> theValue) {
			return new StringCriterion<StringClientParam>(getParamName() + Constants.PARAMQUALIFIER_STRING_CONTAINS, theValue);
		}

		@Override
		public ICriterion<?> values(String... theValues) {
			return new StringCriterion<StringClientParam>(getParamName() + Constants.PARAMQUALIFIER_STRING_CONTAINS, Arrays.asList(theValues));
		}
	}

}
