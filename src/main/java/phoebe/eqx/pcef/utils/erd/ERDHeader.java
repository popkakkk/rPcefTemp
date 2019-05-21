package phoebe.eqx.pcef.utils.erd;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;

@XmlRootElement(name = "ERDHeader")
@XmlAccessorType(XmlAccessType.FIELD)
public class ERDHeader {

    private List<Header> Header;


    public List<Header> getHeaders() {
        return Header;
    }

    public void setHeaders(List<Header> headers) {
        this.Header = headers;
    }
}
