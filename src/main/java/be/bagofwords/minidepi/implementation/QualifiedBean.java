/*******************************************************************************
 * Created by Koen Deschacht (koendeschacht@gmail.com) 2017-3-11. For license
 * information see the LICENSE file in the root folder of this repository.
 ******************************************************************************/

package be.bagofwords.minidepi.implementation;

import java.util.Set;

public class QualifiedBean {

    public final Set<String> qualifiers;
    public final Object bean;

    public QualifiedBean(Set<String> qualifiers, Object bean) {
        this.qualifiers = qualifiers;
        this.bean = bean;
    }

    @Override
    public String toString() {
        return "QualifiedBean{" +
                "qualifiers=" + qualifiers +
                ", bean=" + bean +
                '}';
    }
}
