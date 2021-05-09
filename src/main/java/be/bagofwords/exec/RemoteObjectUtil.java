/*
 * Created by Koen Deschacht (koendeschacht@gmail.com) 2017-5-3. For license
 * information see the LICENSE file in the root folder of this repository.
 */

package be.bagofwords.exec;

import java.io.ByteArrayInputStream;

public class RemoteObjectUtil {

    public static Object loadObject(PackedRemoteObject packedRemoteObject, RemoteObjectClassLoader remoteObjectClassLoader) {
        try {
            remoteObjectClassLoader.addRemoteClasses(packedRemoteObject.classSources);
            ByteArrayInputStream bis = new ByteArrayInputStream(packedRemoteObject.serializedObject);
            RemoteObjectInputStream ois = new RemoteObjectInputStream(bis, remoteObjectClassLoader);
            Object object = ois.readObject();
            ois.close();
            return object;
        } catch (Exception exp) {
            throw new RemoteObjectException("Failed to load " + packedRemoteObject, exp);
        }
    }

}
