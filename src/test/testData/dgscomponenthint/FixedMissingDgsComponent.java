import com.netflix.graphql.dgs.DgsComponent;
import com.netflix.graphql.dgs.DgsQuery;

@DgsComponent
public class MissingDgsComponent {
    @DgsQuery
    public String hello() {
        return "hello";
    }
}