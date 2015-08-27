package christophethiry.com.github.javasrc;

import christophethiry.com.github.javasrc.activedirectory.ActiveDirectoryManager;

public class App {
	public static void main(String[] args) {
		ActiveDirectoryManager adb = new ActiveDirectoryManager("CN=MyUser,OU=Miscellaneous Users,DC=github,DC=com", "mypassword", "DC=github,DC=com");
		try {
			System.out.println(adb.getUser("CTH").get(0).getOrganizations());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
