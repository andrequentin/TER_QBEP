#ifndef POINT_H
#define POINT_H
#include "Vector.h"
class Point {
  protected:
    double x,y,z;
  public:
    Point();
    Point(double xx, double yy, double zz);
    Point( Point &v);

    double getx();
    void setx(double xx);
    double gety();
    void sety(double yy);
    double getz();
    void setz(double zz);

    
    Point* ProjectOnLine(Point &p1Line,Point &p2Line);
    Point* ProjectOnLine(Vector &v,Point &pLine);
    Point* ProjectOnPlane(Point &p1Plane,Vector &nOfPlane);

};

#endif
