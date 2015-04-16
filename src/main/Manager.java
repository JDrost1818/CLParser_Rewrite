package main;

import data.CraigslistUrls;
import dataHandlers.Email;
import dataHandlers.JSoupAddOn;
import objects.Post;
import objects.Search;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;

public class Manager {

    private boolean testing = false;
    private String username = "";
    private String password = "";
    private String toEmail = "";

    public static void main(String[] args) {
        Manager app = new Manager();
        app.run(args);
    }

    private ArrayList<Search> searches = new ArrayList<Search>();

    public Manager() {
        addSearch(new Search(CraigslistUrls.VIDEO_GAMING.owner(), "minneapolis", "XBOX ONE"));
        addSearch(new Search(CraigslistUrls.ELECTRONICS.owner(), "minneapolis", "TV -projection"));
    }

    public void run(String[] args) {
        if (args.length > 0) {
            testing = true;
            username = args[0];
            password = args[1];
            toEmail = args[2];
        }

        ArrayList<Post> newPosts = parsePages(this.searches);

        if (newPosts.size() > 0 && !username.equals(""))
            this.emailPosts(this.username, this.password, this.toEmail, newPosts);
    }

    private void emailPosts(String user, String pass, String toEmail, ArrayList<Post> newPosts) {
        String message = "";

        for (Search curSearch : this.searches) {
            // Creates heading for each search category
            message += CraigslistUrls.titleFromKey(curSearch.category()) + "\n\n";
            for (Post curPost : newPosts) {
                if (curPost.isFromSearch(curSearch)) {
                    message += "\t$" +  curPost.price() + " - " +
                                        curPost.title() + " " +
                                        curPost.link() + "\n";
                    newPosts.remove(curPost);
                }
            }
            message += "\n";
        }
        Email.sendMail(user, pass, toEmail, message);
    }

    public void addSearch(Search newSearch) {
        searches.add(newSearch);
    }

    /*  Parses over a Craigslist page and runs until no more pages, or settings
        defined by user indicate a stop is required. Gathers as much data as
        possible while doing so. Returns a list of new posts.
    */
    public ArrayList<Post> parsePages(ArrayList<Search> searches) {
        double curTime = System.currentTimeMillis();

        ArrayList<Post> newPosts = new ArrayList<Post>();
        Document doc;

        int i, numEntries, numAlreadyVisited;

        for (Search curSearch : searches) {
            // Reset variables
            i = 0; numEntries = 100; numAlreadyVisited = 0;

            // Iterate over whole pages
            while (numEntries == 100 && numAlreadyVisited < 25) {
                numAlreadyVisited = 0;

                System.out.println("Scanning Page " + (++i) + " @ " + curSearch.searchUrl(i));
                doc = JSoupAddOn.connect(curSearch.searchUrl(i));

                if (doc == null) {
                    System.out.println("Could not connect, moving on");
                    break;
                } else {
                    // Gets the number of posts found and isolates
                    // the posts by trimming the document to only them
                    numEntries = (testing) ? 5 : doc.select("p.row").size();
                    Elements allPosts = doc.select("div.content");
                    Element curPost;

                    // Iterates through all the posts found for the page
                    // Currently just prints all relevant information
                    for (int j = 0; j < numEntries; j++) {
                        curPost = allPosts.select("p.row").get(j);
                        if (curSearch.alreadyVisited(curPost.attr("data-pid"))) {
                            numAlreadyVisited++;
                        } else {
                            newPosts.add(new Post(curSearch, curPost));
                            curSearch.addPage(curPost.attr("data-pid"));

                        }
                    }
                }
            }
        }
        System.out.println("Process took " + (System.currentTimeMillis() - curTime) / 1000 + " seconds");
        System.out.println(newPosts.get(1));
        return newPosts;
    }
}