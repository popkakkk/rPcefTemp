package phoebe.eqx.pcef.core.cdr;


import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "Opudr")
@XmlAccessorType(XmlAccessType.FIELD)
public class Opudr {

    private Ctudr ctudr;

    public Ctudr getCtudr() {
        return ctudr;
    }

    public void setCtudr(Ctudr ctudr) {
        this.ctudr = ctudr;
    }
}
