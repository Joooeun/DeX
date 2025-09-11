package kr.Windmill.pages;

import org.openqa.selenium.By;

public class SQLTemplatePage {
    public static final By UNCATEGORIZED_CATEGORY = By.cssSelector("[data-id='UNCATEGORIZED']");
    public static final By TEMPLATE_ITEMS = By.cssSelector(".template-item");
    public static final By NEW_TEMPLATE_BUTTON = By.cssSelector("[data-testid='new-template-button']");
    public static final By SAVE_BUTTON = By.cssSelector("[data-testid='save-template-button']");
    public static final By SQL_NAME_FIELD = By.id("sqlTemplateName");
    public static final By SQL_DESC_FIELD = By.id("sqlTemplateDesc");
    public static final By TEMPLATE_LIST = By.id("templateList");
    public static final By CATEGORY_LIST = By.id("categoryList");
    public static final By CONNECTION_SELECT = By.id("connectionlist");
}
