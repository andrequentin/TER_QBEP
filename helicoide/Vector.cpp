#include "Vector.h"
#include <cmath>


    Vector::Vector(){
      this->setx(0);
      this->setz(0);
      this->sety(0);
    }
      Vector::Vector(double xx, double yy, double zz){
        this->setx(xx);
        this->setz(zz);
        this->sety(yy);
      }
      Vector::Vector( Vector &v){
        this->setx(v.getx());
        this->setz(v.getz());
        this->sety(v.gety());
      }



      void   Vector::normalize(){
        double norme= this->norme();
        this->setx(this->getx()/norme);
        this->setz(this->getz()/norme);
        this->sety(this->gety()/norme);
      }
      double   Vector::norme(){
        return sqrt(pow(this->getx(),2) + pow(this->getz(),2) + pow(this->gety(),2));
      }
      double   Vector::scalar(Vector &v2){
        return (this->getx()*v2.getx()+this->getz()*v2.getz()+this->gety()*v2.gety());
      }
      Vector*   Vector::vectoriel(Vector &v2){
        return new Vector(this->gety()*v2.getz()-this->getz()*v2.gety(),
            this->getz()*v2.getx() - this->getx()*v2.getz()  ,
            this->getx()*v2.gety() - this->gety()*v2.getx()  );
      }
      double   Vector::angle(Vector &v2){
        return acos(this->scalar(v2) / (this->norme() * v2.norme()));
      }

    double   Vector::getx(){
      return this->x;
    }
    void   Vector::setx(double xx){
      this->x=xx;
    }
    double   Vector::gety(){
      return this->y;
    }
    void   Vector::sety(double yy){
      this->y=yy;

    }
    double   Vector::getz(){
      return this->z;

    }
    void   Vector::setz(double zz){
      this->z=zz;

    }
