import com.netflix.graphql.dgs.DgsQuery;

public class <warning descr="A class should be annotated @DgsComponent when DGS annotations are used within the class">MissingDgsComponent<caret></warning> {
    @DgsQuery
    public String hello() {
        return "hello";
    }
}