package adonai.diary_browser.entities;

public class TagsPage extends PostsPage
{
    @Override
    public String get_page_URL()
    {
        return super.get_page_URL() + "?tags";
    }
}
