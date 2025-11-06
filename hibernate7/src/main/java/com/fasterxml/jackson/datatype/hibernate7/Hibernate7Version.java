package com.fasterxml.jackson.datatype.hibernate7;

public class Hibernate7Version {

    public static String getHibernateVersion(){
        try {
            return Class.forName("org.hibernate.Version").getPackage().getImplementationVersion();
        } catch (Exception e) {
            // Should not happen: hibernate not found in the classpath
            throw new RuntimeException(e);
        }
    }

    public static boolean isHibernate7_Plus(){
        String version = getHibernateVersion();
        String[] split = version.split("\\.");
        return split[0].compareTo("7") == 0;
    }

    public static Class<?> getTransactionCoordinatorClass() {
        try {
            return Class.forName("org.hibernate.resource.transaction.TransactionCoordinator");
        } catch (ClassNotFoundException e) {
            try {
                return Class.forName("org.hibernate.resource.transaction.spi.TransactionCoordinator");
            } catch (Exception e2) {
                // should never happen
                throw new RuntimeException(e); 
            }
        }
    }

}
