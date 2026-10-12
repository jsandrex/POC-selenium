package com.saucedemo;

import java.time.Duration;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class SauceDemoTests {

	private static final String BASE_URL = "https://www.saucedemo.com/";
	private static final String VALID_USERNAME = "standard_user";
	private static final String VALID_PASSWORD = "secret_sauce";

	private WebDriver driver;
	private WebDriverWait wait;

	@BeforeMethod
	public void setUp() {
		ChromeOptions options = new ChromeOptions();
		options.addArguments("--window-size=1280,900");
		options.setExperimentalOption("prefs", java.util.Map.of(
				"credentials_enable_service", false,
				"profile.password_manager_enabled", false,
				"profile.password_manager_leak_detection", false));
		if (Boolean.getBoolean("headless")) {
			options.addArguments("--headless=new");
		}
		driver = new ChromeDriver(options);
		wait = new WebDriverWait(driver, Duration.ofSeconds(15));
	}

	@AfterMethod(alwaysRun = true)
	public void tearDown() {
		if (driver != null) {
			driver.quit();
			driver = null;
		}
	}

	private WebElement visible(By locator) {
		return wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
	}

	private void click(By locator) {
		wait.until(ExpectedConditions.elementToBeClickable(locator)).click();
	}

	private void fill(By locator, String value) {
		WebElement input = visible(locator);
		input.click();
		input.clear();
		input.sendKeys(value);
		wait.until(ExpectedConditions.attributeToBe(locator, "value", value));
	}

	private void assertText(By locator, String expectedText) {
		visible(locator);
		wait.until(ExpectedConditions.textToBe(locator, expectedText));
		Assert.assertEquals(driver.findElement(locator).getText(), expectedText);
	}

	private void login() {
		login(VALID_USERNAME, VALID_PASSWORD);
	}

	private void login(String username, String password) {
		driver.get(BASE_URL);
		visible(By.cssSelector("[data-test='username']")).sendKeys(username);
		visible(By.cssSelector("[data-test='password']")).sendKeys(password);
		click(By.cssSelector("[data-test='login-button']"));
	}

	@Test(description = "TC_SAUCE_01 - Login with valid credentials")
	public void tcSauce01LoginWithValidCredentials() {
		login();
		wait.until(ExpectedConditions.urlContains("inventory.html"));
		Assert.assertTrue(driver.getCurrentUrl().contains("inventory.html"));
		assertText(By.cssSelector(".title"), "Products");
		Assert.assertTrue(visible(By.id("react-burger-menu-btn")).isDisplayed());
	}

	@Test(description = "TC_SAUCE_02 - Reject a locked out user")
	public void tcSauce02RejectLockedOutUser() {
		login("locked_out_user", VALID_PASSWORD);
		assertText(By.cssSelector("[data-test='error']"),
				"Epic sadface: Sorry, this user has been locked out.");
	}

	@Test(description = "TC_SAUCE_03 - Reject invalid login credentials")
	public void tcSauce03RejectInvalidCredentials() {
		login("invalid_user", "wrong_password");
		assertText(By.cssSelector("[data-test='error']"),
				"Epic sadface: Username and password do not match any user in this service");
	}

	@Test(description = "TC_SAUCE_04 - Sort products by price from low to high")
	public void tcSauce04SortProductsByPriceLowToHigh() {
		login();
		new Select(visible(By.cssSelector("[data-test='product-sort-container']")))
				.selectByValue("lohi");
		assertText(By.cssSelector(".inventory_item_name"), "Sauce Labs Onesie");
		assertText(By.cssSelector(".inventory_item_price"), "$7.99");
	}

	@Test(description = "TC_SAUCE_05 - Add and remove an item from the cart")
	public void tcSauce05AddAndRemoveCartItem() {
		login();
		click(By.cssSelector("[data-test='add-to-cart-sauce-labs-backpack']"));
		assertText(By.cssSelector(".shopping_cart_badge"), "1");
		click(By.cssSelector(".shopping_cart_link"));
		assertText(By.cssSelector(".inventory_item_name"), "Sauce Labs Backpack");

		click(By.cssSelector("[data-test='remove-sauce-labs-backpack']"));
		wait.until(ExpectedConditions.numberOfElementsToBe(
				By.cssSelector(".shopping_cart_badge"), 0));
		Assert.assertTrue(visible(By.cssSelector(".shopping_cart_link")).isDisplayed());
		By backpackName = By.xpath("//*[contains(@class, 'inventory_item_name')"
				+ " and normalize-space()='Sauce Labs Backpack']");
		wait.until(ExpectedConditions.numberOfElementsToBe(backpackName, 0));
		Assert.assertEquals(driver.findElements(backpackName).size(), 0);
	}

	@Test(description = "TC_SAUCE_06 - Complete checkout")
	public void tcSauce06CompleteCheckout() {
		login();
		click(By.cssSelector("[data-test='add-to-cart-sauce-labs-backpack']"));
		assertText(By.cssSelector(".shopping_cart_badge"), "1");
		click(By.cssSelector(".shopping_cart_link"));
		click(By.cssSelector("[data-test='checkout']"));

		fill(By.cssSelector("[data-test='firstName']"), "Taylor");
		fill(By.cssSelector("[data-test='lastName']"), "Tester");
		fill(By.cssSelector("[data-test='postalCode']"), "90210");
		click(By.cssSelector("[data-test='continue']"));
		assertText(By.cssSelector(".title"), "Checkout: Overview");

		click(By.cssSelector("[data-test='finish']"));
		assertText(By.cssSelector(".complete-header"), "Thank you for your order!");
	}
}