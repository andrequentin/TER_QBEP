#ifndef VECTOR_H
#define VECTOR_H
class Vector {
  protected:
    double x,y,z;
  public:
    Vector();
    Vector(double xx, double yy, double zz);
    Vector( Vector &v);

    double getx();
    void setx(double xx);
    double gety();
    void sety(double yy);
    double getz();
    void setz(double zz);
    void normalize();
    double norme();
    double scalar(Vector &v2);
    Vector* vectoriel(Vector &v2);
    double angle(Vector &v2);
};

#endif
