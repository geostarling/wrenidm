/** 
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2014 ForgeRock AS. All rights reserved.
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://forgerock.org/license/CDDLv1.0.html
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at http://forgerock.org/license/CDDLv1.0.html
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 */

/*
 * This script removes an attribute value from the existing target attribute and returns the new target attribute value
 * 
 * The following variables are supplied: 
 *   targetObject, sourceObject, existingTargetObject, attributeName, attributeValue
 */

var result = {
        "value" : null
    };

function removeValues(target, name, value) {
    if (target[name] != null) {
        var targetValue = target[name];
        if (targetValue instanceof Array) {
            for (var x = 0; x < value.length; x++) {
                var index = targetValue.indexOf(value[x]);
                if (index > -1) {
                    targetValue.splice(index, 1);
                }
            }
        } else if (targetValue instanceof Object) {
            if (targetValue.hasOwnProperty(name)) {
                delete targetValue[name];
            }
        } else {
            target[name] = null;
        }
    }
}

if (existingTargetObject !== null && existingTargetObject !== undefined) {
    removeValues(existingTargetObject, attributeName, attributeValue);
    result.value = existingTargetObject[attributeName];
}

result;
