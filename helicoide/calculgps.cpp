#include <iostream>
#include <iomanip>
#include <cmath>
#include  "GpsPt.cpp"
using namespace std;
# define PI   3.14159265358979323846


double meterToRad(double dis){
  double r = 1.0 ;
  r=r/60;
  r=r/ (180/PI);
  r= r*(dis/1852);
  return r;
}
double degtorad(double d){
  return d * PI/180;

}
double radtodeg(double r){
  return r * 180 / PI;

}
double realMod(double val,double modval){
  double res=fmod(val,modval);
  if(res<0){
    res+=modval;
  }
  return res;
}
GpsPt getP2fromP1(GpsPt p1,double az, double raddist){
  GpsPt res;
  res.lat=asin(sin(p1.lat)*cos(raddist)+cos(p1.lat)*sin(raddist)*cos(az));
  res.lng=realMod(p1.lng-(atan2(sin(az)*sin(raddist)*cos(p1.lat),cos(raddist)-sin(p1.lat)*sin(res.lat))+PI), PI/2)  ;
  return res;
}
int main(){
  cout<< setprecision(17);
  cout<<"nb points"<<endl;
  int uu;
  cin>>uu;
  cout<<"distance"<<endl;
  double dis;
  cin>>dis;

  GpsPt p1;
  cout<<"lat"<<endl;
  cin>>p1.lat;
  cout<<p1.lat<<endl;
  cout<<"long"<<endl;
  cin >>p1.lng;

  double  rads[uu];
  GpsPt pts[uu];
  double angle=((2*PI)/uu);
  double a=angle;

  GpsPt p1rad;
  p1rad.lat=degtorad(p1.lat);
  p1rad.lng=degtorad(p1.lng);
  for(int i =0;i<uu;i++){
    rads[i]=a;
    pts[i]=getP2fromP1(p1rad,rads[i],meterToRad(dis));

    cout<<"------"<<endl;
    cout<<radtodeg(pts[i].lat)<<endl;
    cout<<radtodeg(pts[i].lng)<<endl;
    a+=angle;
  }



}
