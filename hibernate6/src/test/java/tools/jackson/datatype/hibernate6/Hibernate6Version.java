package tools.jackson.datatype.hibernate6;

public class Hibernate6Version
{
    public static String getHibernateVersion() {
        try {
            // Use Version.getVersionString() instead of Package.getImplementationVersion()
            // because the latter returns null in JPMS/module-info contexts
            Class<?> versionClass = Class.forName("org.hibernate.Version");
            return (String) versionClass.getMethod("getVersionString").invoke(null);
        } catch (Exception e) {
            // Should not happen: hibernate not found in the classpath
            throw new RuntimeException(e);
        }
    }

    public static boolean isHibernate6_Plus() {
        String version = getHibernateVersion();
        String[] split = version.split("\\.");
        return split[0].compareTo("6") == 0;
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
