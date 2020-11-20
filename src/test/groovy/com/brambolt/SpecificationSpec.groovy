package com.brambolt

class SpecificationSpec extends spock.lang.Specification {

  def 'can get boolean'() {
    given:
    Properties defaults = new Properties()
    String bKey = 'p1'
    boolean bValue = true
    defaults.setProperty(bKey, Boolean.toString(bValue))
    Specification spec = new Specification(defaults)
    when:
    Boolean v = spec.getBoolean(bKey)
    then:
    bValue == v
  }

  def 'can get integer'() {
    given:
    Properties defaults = new Properties()
    String intKey = 'p1'
    int intValue = 1
    defaults.setProperty(intKey, Integer.toString(intValue))
    Specification spec = new Specification(defaults)
    when:
    Integer v = spec.getInteger(intKey)
    then:
    intValue == v
  }
}
