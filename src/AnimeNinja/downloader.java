package AnimeNinja;

import java.awt.AWTException;
import java.awt.Image;
import java.awt.SystemTray;
import java.awt.Toolkit;
import java.awt.TrayIcon;
import java.awt.TrayIcon.MessageType;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;
import javax.swing.filechooser.FileSystemView;
import org.openqa.selenium.By;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.openqa.selenium.WebDriver;

public class downloader implements Runnable{

	protected static WebDriver driver2;
	private static HashMap<String, Object> chromePrefs = new HashMap<String, Object>();
	protected static ArrayList<String> megaLinks = new ArrayList<String>();
	protected static ArrayList<String> driveLinks = new ArrayList<String>();
	private TrayIcon trayIcon;
	private static File defaultDir;
	@Override
	public void run()
	{
		setup();
		removeOldUncompletedDownloads();
		updater.update();
		showTrayIcon();
		megaDownloader();
	}

	private void driveDownloader()
	{
		String epName = null;
		String fullSize = null;
		if(driveLinks.size() != 0)
		{
			driver2.get(driveLinks.get(0));
			driveLinks.remove(0);
			try {
				driver2.findElement(By.tagName("p"));
				try {
					driver2.findElement(By.id("uc-download-link")).click();
					String nameAndSize = driver2.findElement(By.cssSelector(".uc-name-size")).getText();
					fullSize = nameAndSize.replaceAll("(.+\\()", "").replaceAll("\\)", "B");
					epName = nameAndSize.replaceAll("\\[AnimeSanka.com]\\s|\\s\\(.+", "");
					gui.lblNewLabel_3.setVisible(false);
					gui.lblNewLabel_3.setText(epName);
					gui.lblNewLabel_3.setVisible(true);
				}catch(Exception e) {
					gui.lblNewLabel_7.setText("Failed to download EP "+(gui.selectedEpisode+1)+" (Old Links)");
					guiRefresh();
					notifier("Download Failed (Old Links)","EP "+gui.selectedEpisode+1);
				}
			}catch(Exception e) {
				gui.lblNewLabel_7.setText("Failed to download EP "+(gui.selectedEpisode+1)+" (Old Links)");
				guiRefresh();
				notifier("Download Failed (Old Links)","EP "+gui.selectedEpisode+1);
				return;
			}
			File tmpDownload = getTempDownloadFile();
			double downloadInfo;
			while(true)
			{
				downloadInfo = tmpDownload.length()/(1024*1024);
				if (tmpDownload.exists())
				{
					System.out.println("finding download info");
					System.out.println(fullSize);
					gui.lblNewLabel_7.setText(String.valueOf(downloadInfo)+" MB of "+fullSize);
					gui.lblNewLabel_5.setText("Files left:"+driveLinks.size());
					guiRefresh();
				}
				else
				{
					gui.lblNewLabel_7.setText("Download Completed");
					guiRefresh();
					notifier("Download Completed",epName);
					break;
				}
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
		try {
			Thread.sleep(500);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	private File getTempDownloadFile()
	{
		File tempDownload = null;
		boolean found = false;
		while(!found)
		{
			File[] files = defaultDir.listFiles();
			for(int i=0 ; i<files.length ; i++)
			{
				if(files[i].getName().contains("crdownload"))
				{
					tempDownload = files[i];
					found = true;
					break;
				}
			}
		}
		System.out.println("found the file");
		return tempDownload;
	}
	
	private void removeOldUncompletedDownloads()
	{
		File[] files = defaultDir.listFiles();
		for(int i=0 ; i<files.length ; i++)
		{
			if(files[i].getName().contains("crdownload"))
				files[i].delete();
		}
	}
	
	private void megaDownloader()
	{
		String epName = "";
		while(true)
		{
			if(megaLinks.size()!=0)
			{
			    driver2.get(megaLinks.get(0));
				megaLinks.remove(0);

			    try {
			    	WebDriverWait wait = new WebDriverWait(driver2, 15);
					wait.until(ExpectedConditions.invisibilityOfElementLocated(By.id("loading")));
				    driver2.findElement(By.cssSelector(".download.big-button.button.download-file.green.transition span")).click();
				}catch(Exception e) {
					driveDownloader();
					continue;
				}
				while(true)
				{
					if(!(driver2.findElement(By.cssSelector(".download.progress-bar")).getAttribute("style").contains("100%")))
					{
						try {
							driver2.findElement(By.cssSelector(".top-login-popup.sign.fm-dialog.pro-register-dialog.hidden"));
						}catch(Exception e) {
							while(true)
							{
								driveDownloader();
							}
						}
						try {
							String speedInfo = driver2.findElement(By.cssSelector(".dark-numbers")).getText()
									+" "+driver2.findElement(By.cssSelector(".light-txt")).getText();
							epName = driver2.findElement(By.cssSelector(".download.bar-filename")).  ////*without website name*////
									getAttribute("title").replaceAll("\\[[^]]+\\]", "");
							String sizeInfo = driver2.findElement(By.cssSelector(".download.bar-filesize")).getText().replaceFirst("B", "B of ");
							gui.lblNewLabel_3.setVisible(false);
							gui.lblNewLabel_3.setText("epName : "+epName);
							gui.lblNewLabel_3.setVisible(true);
							gui.lblNewLabel_7.setText(speedInfo+" - "+sizeInfo);
							gui.lblNewLabel_5.setText("Files left: "+megaLinks.size());
							guiRefresh();
						}catch(Exception e) {
							finishingDownload();
							notifier("Downloade Completed",epName);
							driveLinks.remove(0);
							break;
						}
						try {
							Thread.sleep(1000);
						} catch (InterruptedException e1) {
							e1.printStackTrace();
						}
					}
					else
					{
						finishingDownload();
						notifier("Download Completed",epName);
						driveLinks.remove(0);
						break;
					}
				}
			}else if(megaLinks.size() == 0 && driveLinks.size() != 0)
			{
				driveDownloader();
			}
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
		}
	}

	private void guiRefresh()
	{
		gui.lblNewLabel_7.setVisible(false);
		gui.lblNewLabel_7.setVisible(true);
		gui.lblNewLabel_5.setVisible(false);
		gui.lblNewLabel_5.setVisible(true);
	}

	private void finishingDownload()
	{
		for(int i=15 ; i>=0 ; i--)
		{
			gui.lblNewLabel_7.setText("finishing download in "+i+" seconds");
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			guiRefresh();
		}
		gui.lblNewLabel_7.setText("Download Completed");
		guiRefresh();
	}

	private void notifier(String downloadState ,String epName)
	{
		try{
		    trayIcon.displayMessage(downloadState,epName, MessageType.NONE);
		}catch(Exception ex){
		    ex.printStackTrace();
		}
	}

	private void showTrayIcon()
	{
		SystemTray tray = SystemTray.getSystemTray();
	    Image image = Toolkit.getDefaultToolkit().createImage(getClass().getResource("/appIcon.png"));
	    trayIcon = new TrayIcon(image, "Java AWT Tray Demo");
	    trayIcon.setImageAutoSize(true);
	    trayIcon.setToolTip("Anime Downloader");
	    try {
			tray.add(trayIcon);
		} catch (AWTException e) {
			e.printStackTrace();
		}
	}

	private void setup()
	{
		ChromeOptions chromeOptions = new ChromeOptions();
		System.setProperty("webdriver.chrome.driver", System.getenv("SystemDrive")+"\\Program Files\\Anime Ninja\\chromedriver79.exe");
		chromePrefs.put("profile.default_content_settings.popups", 0);
		chromePrefs.put("profile.default_content_setting_values.notifications", 2);
		chromePrefs.put("safebrowsing.enabled", "false");
		chromePrefs.put("download.prompt_for_download", "false");
		setDownloadLocation();
		chromeOptions.setExperimentalOption("prefs", chromePrefs);
	    chromeOptions.addArguments("--headless");
	    chromeOptions.addArguments("--disable-gpu");
	    chromeOptions.addArguments("--unlimited-storage");
		driver2 = new ChromeDriver(chromeOptions);
	    driver2.manage().timeouts().implicitlyWait(2, TimeUnit.SECONDS);
	}

	protected static void setDownloadLocation()
	{
		FileReader reader;
		defaultDir = new File(FileSystemView.getFileSystemView().getHomeDirectory().getAbsolutePath()+"\\Anime Ninja");
		try {
			reader = new FileReader(new File(System.getenv("SystemDrive")+"\\Program Files\\Anime Ninja\\Download Location.txt"));
			BufferedReader br = new BufferedReader(reader);
			defaultDir = new File(br.readLine());
			br.close();
			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		chromePrefs.put("download.default_directory", defaultDir.getAbsolutePath());
		updater.getdownloadLocation(defaultDir);
	}

	protected static void close()
	{
		driver2.close();
		driver2.quit();
	}
}
