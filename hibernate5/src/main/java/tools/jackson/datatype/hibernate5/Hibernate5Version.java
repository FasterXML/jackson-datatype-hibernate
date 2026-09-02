package tools.jackson.datatype.hibernate5;

public class Hibernate5Version {

    public static String getHibernateVersion(){
        try {
            return Class.forName("org.hibernate.Version", false, Hibernate5Version.class.getClassLoader())
                    .getPackage().getImplementationVersion();
        } catch (Exception e) {
            // Should not happen: hibernate not found in the classpath
            throw new RuntimeException(e);
        }
    }

    public static boolean isHibernate5_2_Plus(){
        String version = getHibernateVersion();
        String[] split = version.split("\\.");
        int isV5 = split[0].compareTo("5");
        if(isV5 != 0){
            return isV5 > 0;
        }
        int isV52 = split[1].compareTo("2");
        return isV52 >= 0;
    }

    public static Class<?> getTransactionCoordinatorClass() {
        try {
            return Class.forName("org.hibernate.resource.transaction.TransactionCoordinator",
                    false, Hibernate5Version.class.getClassLoader());
        } catch (ClassNotFoundException e) {
            try {
                return Class.forName("org.hibernate.resource.transaction.spi.TransactionCoordinator",
                        false, Hibernate5Version.class.getClassLoader());
            } catch (Exception e2) {
                // should never happen
                throw new RuntimeException(e); 
            }
        }
    }

}
