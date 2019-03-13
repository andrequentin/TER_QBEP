#include "Point.h"
#include"Vector.cpp"
#include <cmath>


    Point::Point(){
      this->setx(0);
      this->setz(0);
      this->sety(0);
    }
      Point::Point(double xx, double yy, double zz){
        this->setx(xx);
        this->setz(zz);
        this->sety(yy);
      }
      Point::Point( Point &v){
        this->setx(v.getx());
        this->setz(v.getz());
        this->sety(v.gety());
      }

      Point* Point::ProjectOnLine(Point &p1Line,Point &p2Line){
        Vector *v1= new Vector(p2Line.getx()-p1Line.getx(),p2Line.gety()-p1Line.gety(),p2Line.getz()-p1Line.getz());
        v1->normalize();
        Vector *v2= new Vector(this->getx()-p1Line.getx(),this->gety()-p1Line.gety(),this->getz()-p1Line.getz());
        double b = v1->scalar(*v2)/v1->norme();
        return new Point(p1Line.getx()+v1->getx()*b ,p1Line.gety()+v1->gety()*b ,p1Line.getz()+v1->getz()*b );
      }

      Point* Point::ProjectOnLine(Vector &v,Point &pLine){
        v.normalize();
        Vector *v2= new Vector(this->getx()-pLine.getx(),this->gety()-pLine.gety(),this->getz()-pLine.getz());
        double b = v.scalar(*v2)/v.norme();
        return new Point(pLine.getx()+v.getx()*b ,pLine.gety()+v.gety()*b ,pLine.getz()+v.getz()*b );

      }

      Point* Point::ProjectOnPlane(Point &p1Plane,Vector &nOfPlane){
        Vector *v2= new Vector(this->getx()-p1Plane.getx(),this->gety()-p1Plane.gety(),this->getz()-p1Plane.getz());
        double b = v2->scalar(nOfPlane)/v2->norme();
        return new Point(this->getx() - nOfPlane.getx()*b,this->gety() - nOfPlane.gety()*b,this->getz() - nOfPlane.getz()*b );
      }




      double   Point::getx(){
        return this->x;
      }
      void   Point::setx(double xx){
        this->x=xx;
      }
      double   Point::gety(){
        return this->y;
      }
      void   Point::sety(double yy){
        this->y=yy;

      }
      double   Point::getz(){
        return this->z;

      }
      void   Point::setz(double zz){
        this->z=zz;
      }
