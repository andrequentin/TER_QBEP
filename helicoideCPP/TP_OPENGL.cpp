///////////////////////////////////////////////////////////////////////////////
// ----------------------------------------------------------------------------
// Imagina
// ----------------------------------------------------------------------------
///////////////////////////////////////////////////////////////////////////////
 #include <string>
#include <stdio.h>
#include <iostream>
#include <stdlib.h>
#include <math.h>
#include "Point.cpp"
#include <fstream>


/* Dans les salles de TP, vous avez g�n�ralement acc�s aux glut dans C:\Dev. Si ce n'est pas le cas, t�l�chargez les .h .lib ...
Vous pouvez ensuite y faire r�f�rence en sp�cifiant le chemin dans visual. Vous utiliserez alors #include <glut.h>.
Si vous mettez glut dans le r�pertoire courant, on aura alors #include "glut.h"
*/

#include <GL/glut.h>

// D�finition de la taille de la fen�tre
#define WIDTH  600

#define HEIGHT 600

// D�finition de la couleur de la fen�tre
#define RED   1
#define GREEN 1
#define BLUE  1
#define ALPHA 1
# define PI   3.14159265358979323846

// Touche echap (Esc) permet de sortir du programme
#define KEY_ESC 27

using namespace std;
int nmed=10;
int npar=10;
float u = 0.5;
float v = 0.0;
float step = 0.01;


// Ent�tes de fonctions
void init_scene();
void render_scene();
GLvoid initGL();
GLvoid window_display();
GLvoid window_reshape(GLsizei width, GLsizei height);
GLvoid window_key(unsigned char key, int x, int y);

GLvoid DrawPoint(Point &p);
GLvoid DrawLine(Vector &v);

int main(int argc, char **argv)
{
  // initialisation  des param�tres de GLUT en fonction
  // des arguments sur la ligne de commande
  glutInit(&argc, argv);
  glutInitDisplayMode(GLUT_RGBA);
  glutInitDisplayMode(GLUT_DEPTH);

  // d�finition et cr�ation de la fen�tre graphique, ainsi que son titre
  glutInitWindowSize(WIDTH, HEIGHT);
  glutInitWindowPosition(0, 0);
  glutCreateWindow("Lumière");

  // initialisation de OpenGL et de la sc�ne
  initGL();
  init_scene();

  // choix des proc�dures de callback pour
  // le trac� graphique
  glutDisplayFunc(&window_display);

  // le redimensionnement de la fen�tre
  glutReshapeFunc(&window_reshape);
  // la gestion des �v�nements clavier
  glutKeyboardFunc(&window_key);

  // la boucle prinicipale de gestion des �v�nements utilisateur
  glutMainLoop();

  return 1;
}



// initialisation du fond de la fen�tre graphique : noir opaque
GLvoid initGL()
{
  glClearColor(RED,GREEN , BLUE, ALPHA);
}

void grid(){
  glColor3f(0,0,0);
  glBegin(GL_POINTS);

  glPointSize(3);
  for(int i = -12;i<12;i++){
    for(int j = -12;j<12;j++){
      for(int k = -12;k<12;k++){
          glVertex3f(i,j,k);
      }
    }
  }
  glEnd();

}


// Initialisation de la scene. Peut servir � stocker des variables de votre programme
// � initialiser

void init_scene()
{


  // float eyeX = 4.0;
  // float eyeY = 4.0;
  // float eyeZ = 4.0;

glEnable(GL_DEPTH_TEST);
//glEnable(GL_TEXTURE_2D);

}

// fonction de call-back pour l�affichage dans la fen�tre

GLvoid window_display()
{
  glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);/*  glColor3f(1.0, 0.0, 0.0);
  DrawLine(*v0);
  glColor3f(0.0, 1.0, 0.0);.
  DrawLine(*v1);
*/
  glLoadIdentity();

  // C'est l'endroit o� l'on peut dessiner. On peut aussi faire appel
  // � une fonction (render_scene() ici) qui contient les informations
  // que l'on veut dessiner
  render_scene();

  // trace la sc�ne grapnique qui vient juste d'�tre d�finie
  glFlush();
}

// fonction de call-back pour le redimensionnement de la fen�tre

GLvoid window_reshape(GLsizei width, GLsizei height)
{
  glViewport(0, 0, width, height);

  glMatrixMode(GL_PROJECTION);
  glLoadIdentity();
  // ici, vous verrez pendant le cours sur les projections qu'en modifiant les valeurs, il est
  // possible de changer la taille de l'objet dans la fen�tre. Augmentez ces valeurs si l'objet est
  // de trop grosse taille par rapport � la fen�tre.
  glOrtho(-20.0,
    20.0, -20.0, 20.0, -20.0, 20.0);

  // toutes les transformations suivantes s�appliquent au mod�le de vue
  glMatrixMode(GL_MODELVIEW);
}

// fonction de call-back pour la gestion des �v�nements clavier



GLvoid window_key(unsigned char key, int x, int y)
{
  switch (key) {
  case KEY_ESC:
    exit(1);
    case 100: // D
       u += step;
       if(u > 1.0) {
         u = 0.0;
       }

       break;

       case 97: // A
       u -= step;
       if(u < 0.0) {
         u = 1.0;
       }

       break;
       case 122: // z
         v += step;
         if(v > 1.0) {
           v = 0.0;
         }
    break;

    case 115: // s
         v -= step;
         if(v < 0.0) {
           v = 1.0;
         }
         break;
    default:
      printf ("La touche %d n�est pas active.\n", key);
    break;

  }

   glutPostRedisplay();


}


GLvoid DrawPoint(Point &p){
glPointSize(4);
  glBegin(GL_POINTS);
    glVertex3f(p.getx(), p.gety(), p.getz());
  glEnd();
}
GLvoid DrawLine(Vector &v){

  glBegin(GL_LINES);
    glVertex3f(0, 0, 0);
    glVertex3f(v.getx(), v.gety(), v.getz());
  glEnd();

}
GLvoid DrawCurve(Point TabPointsOfCurve [], long nbPoints){
  glBegin(GL_LINE_STRIP);
  for(int i = 0; i < nbPoints;i++){
    glVertex3f(TabPointsOfCurve[i].getx(), TabPointsOfCurve[i].gety(), TabPointsOfCurve[i].getz());
  }
  glEnd();
}

//////////////////////////////////////////////////////////////////////////////////////////
//////////////////////////////////TP2/////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////////////////

GLvoid HermiteCubicCurve(Point P0, Point P1, Vector V0, Vector V1, long nbU , Point *resultat){


  double f1,f2,f3,f4;
  float u;
for(int i=0;i<nbU;i++){
  u=(float)i/(float)nbU;

  f1= 2 * pow(u,3) - 3 * pow(u,2) + 1;
  f2= -2 * pow(u,3) + 3 * pow(u,2);
  f3= pow(u,3) - 2 * pow(u,2) + u;
  f4= pow(u,3) - pow(u,2);

  Point *p = new Point( f1*P0.getx()+f2*P1.getx()+f3*V0.getx()+f4*V1.getx()  ,
  f1*P0.gety()+f2*P1.gety()+f3*V0.gety()+f4*V1.gety()  ,
  f1*P0.getz()+f2*P1.getz()+f3*V0.getz()+f4*V1.getz());
  resultat[i] = *p;
}
resultat[nbU]= P1;

  return;


}
long fact (long x) {
  if ((x == 1) ||(x==0))
    return 1;
  return (x * fact(x-1));
}

GLvoid BezierCurveByBernstein(Point* PointTabControl , long nbControlPoint, long nbU, Point*resultat){
 long n = nbControlPoint-1;
 long double sommePolyBX, sommePolyBY, sommePolyBZ,  u;
 long double B=0;

 for(long i = 0; i < nbU; i++) {
   u = (double)i/(double)(nbU);
   sommePolyBX = 0;
   sommePolyBY = 0;
   sommePolyBZ = 0;
   B=0;

   for(long j = 0; j <= n; j++)  {
     B = (long double) (fact(n)* pow(u, j) * pow(1 - u, n - j)) / (fact(n - j) * fact(j));

     sommePolyBX += B * PointTabControl[j].getx();
     sommePolyBY += B * PointTabControl[j].gety();
     sommePolyBZ += B * PointTabControl[j].getz();
   }

   resultat[i]= *(new Point(sommePolyBX, sommePolyBY, sommePolyBZ));
 }
 resultat[nbU]=PointTabControl[nbControlPoint-1];
}


void BezierCurveByCasteljau( Point* PointTabControl , long nbControlPoint, long nbU, Point*resultat){

  for(int i=0;i<nbU;i++){
    int nbP=(int)nbControlPoint;
    Point *p1tab=PointTabControl;
    double u = (double)i/(double)(nbU);
    while(nbP>1){
      Point *p2tab=new Point[nbP-1];
      //glBegin(GL_LINE_STRIP);
      for(int j =0;j<nbP;j++){
        Point *p1 = new Point(p1tab[j].getx(),p1tab[j].gety(),p1tab[j].getz());
        Point *p2 = new Point(p1tab[j+1].getx(),p1tab[j+1].gety(),p1tab[j+1].getz());
        p2tab[j] = *(new Point(p1->getx()*(1-u) + p2->getx()*u,p1->gety()*(1-u) + p2->gety()*u,p1->getz()*(1-u) + p2->getz()*u ));
      //  glVertex3f(p2tab[j].getx(),p2tab[j].gety(),p2tab[j].getz());
      }
      //glEnd();
      nbP--;
      p1tab=p2tab;
    }
    resultat[i]=p1tab[0];
  }
  resultat[nbU]=PointTabControl[nbControlPoint-1];
}

//////////////////////////////////////////////////////////////////////////////////////////
//////////////////////////////////TP3/////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////////////////
void drawLine(Vector v,Point p){
	Point *p2 = new Point (p.getx()+v.getx(),p.gety()+v.gety(),p.getz()+v.getz());
	glBegin(GL_LINES);
		glVertex3f(p.getx(), p.gety(), p.getz());
		glVertex3f(p2->getx(), p2->gety(), p2->getz());
	glEnd();
}

Point** surfaceCylindrique(Point b[],int nbPbez, Vector d, int nbv, int nbu){
  Point** tab= new Point*[nbv];
  for(int i=0;i<nbv;i++){
      tab[i] = new Point[nbu];
  }
  for(int i=0;i<nbv;i++){
  double v = (double)i/(double)(nbv);
    for(int j =0;j<nbu;j++){
      double u =  (double)j/(double)(nbu);
      int k = (int)(nbPbez*u);
      tab[i][j] = * new Point(b[k].getx() + d.getx()*v,b[k].gety() + d.gety()*v,b[k].getz() + d.getz()*v);
      //trace de construction
  //    drawLine(d,b[k]);
    }
  }
  return tab;
}
Point** surfaceReglees(Point c1[],int nbp1, Point c2[],int nbp2, int nbv,int nbu){
  Point** tab = new Point*[nbv];
  for(int i=0;i<nbv;i++){
    tab[i] = new Point[nbu];

  }
  for(int i=0;i<nbv;i++){
    double v = (double)i/(double)(nbv);
    for(int j=0;j<nbu+1;j++){
      double u =  (double)j/(double)(nbu);
      int k1 = (int)(nbp1*u);
      int k2 = (int)(nbp2*u);
      tab[i][j] = * new Point((1-v)*c1[k1].getx() + v*c2[k2].getx(),(1-v)*c1[k1].gety() + v*c2[k2].gety(),(1-v)*c1[k1].getz() + v*c2[k2].getz());
    }
  }
    //trace de construction
    // for(int i =0; i < nbu;i++){
    //    glBegin(GL_LINE_STRIP);
    //    glVertex3f(tab[0][i].getx(),tab[0][i].gety(),tab[0][i].getz());
    //    glVertex3f(tab[nbv-1][i].getx(),tab[nbv-1][i].gety(),tab[nbv-1][i].getz());
    //    glEnd();
    //  }

  return tab;
}

Point * bezierCurveByBernsteinStep(  Point * controlPoints, long nbc,   long nbU,  unsigned int iU) {
  if(nbc< 2) {
    //std::cerr << "Utils::bezierCurveByBernstein cannot calculate bezier curve with less than 2 points." << std::endl;
    exit(EXIT_FAILURE);
  }
  unsigned int n = nbc-1;
  double intervalBetweenU = 1.0/(nbU-1);
  //Calcul des points
  double u = iU*intervalBetweenU;
  Point* pu = new Point(0,0,0);
  for(unsigned int i=0; i < nbc; ++i) {
    Point controlPoint = controlPoints[i]; // Copy here;
    double polynome = (fact(n)/(fact(i)*fact(n-i))) * pow(u,i) * pow(1.0-u, n-i);
    pu->setx(pu->getx() +controlPoint.getx()*polynome);
    pu->sety(pu->gety() + controlPoint.gety()*polynome);
    pu->setz(pu->getz() +controlPoint.getz()*polynome);
  }
  return pu;
}

Point ** bezierSurfaceByBernstein(Point ** gridCtrlPoints,  long nbControlU , long nbU, long nbControlV, long nbV) {
    Point** result = new Point*[nbV];

    for(unsigned int iV=0; iV < nbV; ++iV) {
      result[iV] = new Point[nbU];
     for( int iU=0; iU < nbU; ++iU) {
       Point * uControlPoints= new Point[nbControlU];
       for( int iVLine=0; iVLine < nbControlU; ++iVLine) {
         Point* uControlPoint = bezierCurveByBernsteinStep(gridCtrlPoints[iVLine],nbControlV, nbV, iU); // A faire en version mieux sans calculer tous les points pour rien
         uControlPoints[iVLine]=*uControlPoint;
       }
       result[iV][iU]= * bezierCurveByBernsteinStep(uControlPoints,nbControlU, nbU, iV);
     }
   }
   return result;
 }

 GLvoid DrawSommetsCyl_Con(Point ** c,int nMeridiens){


     for(int i=0;i<nMeridiens;i++){
       Point p1 = c[0][i];
       Point p2 =  c[1][i];
       Point p3 = c[0][(i+1)%nMeridiens];
       Point p4 =  c[1][(i+1)%nMeridiens];
       glColor3f(0,1,0);

       glBegin(GL_QUAD_STRIP);
         glVertex3f(p1.getx(), p1.gety(), p1.getz());
         glVertex3f(p2.getx(), p2.gety(), p2.getz());
         glVertex3f(p3.getx(), p3.gety(), p3.getz());
         glVertex3f(p4.getx(), p4.gety(), p4.getz());
       glEnd();
       glColor3f(0,0,0);
       glBegin(GL_LINES);
         glVertex3f(p1.getx(), p1.gety(), p1.getz());
         glVertex3f(p2.getx(), p2.gety(), p2.getz());
         glVertex3f(p1.getx(), p1.gety(), p1.getz());
         glVertex3f(p3.getx(), p3.gety(), p3.getz());
         glVertex3f(p2.getx(), p2.gety(), p2.getz());
         glVertex3f(p4.getx(), p4.gety(), p4.getz());
       glEnd();

     }

 }

Point ** CalculSommetsCylindre(Point c,double r,double h,int mds){
  Point ** res= new Point * [2];
  res[0]=new Point[mds];
  res[1]=new Point[mds];

  double angle=((2*PI)/mds);


  for(unsigned int i=0; i < mds; ++i) {
    double x=(r * cos(angle * i));
    double y=(r * sin(angle * i));
    res[0][i]=* new Point(c.getx()+x, c.gety()+y ,c.getz()+( - h/2.0));
    res[1][i]=* new Point(c.getx()+x, c.gety()+y ,c.getz()+(  h/2.0));
  }
  return res;
}


Point ** CalculSommetsCone(Point sommet,double r,double h ,int mds){

  Point ** res= new Point * [2];
  res[0]=new Point[mds];
  res[1]=new Point[mds];

  double angle=((2*PI)/mds);


  for(unsigned int i=0; i < mds; ++i) {
    double x=(r * cos(angle * i));
    double y=(r * sin(angle * i));
    res[0][i]=* new Point(sommet.getx(), sommet.gety(),sommet.getz());
    res[1][i]=* new Point(x+sommet.getx(),y+ sommet.gety() ,sommet.getz()-h);
  }
  return res;
}

void DrawSphere(Point c, double r, int mds,int par) {
  if(mds >= 3 && par >= 2){

    Point ** res = new Point * [mds];
    for(int i =0;i<mds;i++){
      res[i] = new Point[par];
    }
    //par++;

    for(int i = 0; i<mds; i++){
      for(int j = 0; j<par; j++)   {
        Point* p;
        p= new Point(  c.getx()+r*sin((double)(PI)*j/(par))*cos((double)(2*PI*i)/(mds)),
                              c.gety()+r*sin((double)(PI)*j/(par))*sin((double)(2*PI*i)/(mds)),
                              c.getz()+r*cos((double)(PI)*j/(par)));

        res[i][j]=* p;
      }
    }

    // for (int i=0;i<mds;i++){
    //   res[i][mds-1]= * new Point(c.getx(),c.gety(),c.getz()-r);
    // }


    for(int i = 0; i<mds; i++)  {
      for(int j = 0;j<par-1;j++){

          Point p1 = res[i][j];

          Point p2 =  res[(i+1)%mds][j];

          Point p3 =  res[i][(j+1)%par];

          Point p4 =  res[(i+1)%mds][(j+1)%par];

          // glColor3f(1,0,0);
          //
          // glBegin(GL_QUADS);
          //   glVertex3f(p1.getx(), p1.gety(), p1.getz());
          //   glVertex3f(p2.getx(), p2.gety(), p2.getz());
          //   glVertex3f(p4.getx(), p4.gety(), p4.getz());
          //   glVertex3f(p3.getx(), p3.gety(), p3.getz());
          //
          // glEnd();

          glColor3f(0,0,1);

          glBegin(GL_QUAD_STRIP);
            glVertex3f(p1.getx(), p1.gety(), p1.getz());
            glVertex3f(p2.getx(), p2.gety(), p2.getz());

            glVertex3f(p1.getx(), p1.gety(), p1.getz());
            glVertex3f(p3.getx(), p3.gety(), p3.getz());

            glVertex3f(p2.getx(), p2.gety(), p2.getz());
            glVertex3f(p4.getx(), p4.gety(), p4.getz());

            glVertex3f(p3.getx(), p3.gety(), p3.getz());
            glVertex3f(p4.getx(), p4.gety(), p4.getz());
          glEnd();
          glColor3f(0,0,0);

          glBegin(GL_LINES);
            glVertex3f(p1.getx(), p1.gety(), p1.getz());
            glVertex3f(p2.getx(), p2.gety(), p2.getz());

            glVertex3f(p1.getx(), p1.gety(), p1.getz());
            glVertex3f(p3.getx(), p3.gety(), p3.getz());

            glVertex3f(p2.getx(), p2.gety(), p2.getz());
            glVertex3f(p4.getx(), p4.gety(), p4.getz());

            glVertex3f(p3.getx(), p3.gety(), p3.getz());
            glVertex3f(p4.getx(), p4.gety(), p4.getz());
          glEnd();
      }
    }
  }
}

void cube(Point o,double cote,double color[] ){

  glColor3f(color[0],color[1],color[2]);

  glNormal3f(0.0f,0.0f,-1.0f);

  glBegin(GL_QUADS);
    glVertex3f(o.getx(), o.gety(), o.getz());
    glVertex3f(o.getx()+cote, o.gety(), o.getz());
    glVertex3f(o.getx()+cote, o.gety()+cote, o.getz());
    glVertex3f(o.getx(), o.gety()+cote, o.getz());
  glEnd();

  glNormal3f(0.0f,-1.0f,0.0f);
  glBegin(GL_QUADS);
    glVertex3f(o.getx(), o.gety(), o.getz());
    glVertex3f(o.getx()+cote, o.gety(), o.getz());
    glVertex3f(o.getx()+cote, o.gety(), o.getz()+cote);
    glVertex3f(o.getx(), o.gety(), o.getz()+cote);
  glEnd();

  glNormal3f(-1.0f,0.0f,0.0f);
  glBegin(GL_QUADS);
    glVertex3f(o.getx(), o.gety(), o.getz());
    glVertex3f(o.getx(), o.gety()+cote, o.getz());
    glVertex3f(o.getx(), o.gety()+cote, o.getz()+cote);
    glVertex3f(o.getx(), o.gety(), o.getz()+cote);
  glEnd();

  glNormal3f(1.0f,0.0f,0.0f);
  glBegin(GL_QUADS);
    glVertex3f(o.getx()+cote, o.gety(), o.getz());
    glVertex3f(o.getx()+cote, o.gety()+cote, o.getz());
    glVertex3f(o.getx()+cote, o.gety()+cote, o.getz()+cote);
    glVertex3f(o.getx()+cote, o.gety(), o.getz()+cote);
  glEnd();

  glNormal3f(0.0f,0.0f,1.0f);
  glBegin(GL_QUADS);
    glVertex3f(o.getx(), o.gety(), o.getz()+cote);
    glVertex3f(o.getx()+cote, o.gety(), o.getz()+cote);
    glVertex3f(o.getx()+cote, o.gety()+cote, o.getz()+cote);
    glVertex3f(o.getx(), o.gety()+cote, o.getz()+cote);
  glEnd();

  glNormal3f(0.0f,1.0f,0.0f);
  glBegin(GL_QUADS);
    glVertex3f(o.getx(), o.gety()+cote, o.getz());
    glVertex3f(o.getx()+cote, o.gety()+cote, o.getz());
    glVertex3f(o.getx()+cote, o.gety()+cote, o.getz()+cote);
    glVertex3f(o.getx(), o.gety()+cote, o.getz()+cote);
  glEnd();

  glColor3f(0.0,0.0,0.0);

  glBegin(GL_LINES);
    glVertex3f(o.getx(), o.gety(), o.getz());
    glVertex3f(o.getx()+cote, o.gety(), o.getz());

    glVertex3f(o.getx(), o.gety(), o.getz());
    glVertex3f(o.getx(), o.gety()+cote, o.getz());

    glVertex3f(o.getx(), o.gety(), o.getz());
    glVertex3f(o.getx(), o.gety(), o.getz()+cote);

    glVertex3f(o.getx()+cote, o.gety(), o.getz());
    glVertex3f(o.getx()+cote, o.gety()+cote, o.getz());

    glVertex3f(o.getx()+cote, o.gety(), o.getz());
    glVertex3f(o.getx()+cote, o.gety(), o.getz()+cote);

    glVertex3f(o.getx()+cote, o.gety(), o.getz()+cote);
    glVertex3f(o.getx()+cote, o.gety()+cote, o.getz()+cote);

    glVertex3f(o.getx()+cote, o.gety()+cote, o.getz());
    glVertex3f(o.getx()+cote, o.gety()+cote, o.getz()+cote);

    glVertex3f(o.getx(), o.gety()+cote, o.getz());
    glVertex3f(o.getx(), o.gety()+cote, o.getz()+cote);

    glVertex3f(o.getx(), o.gety(), o.getz()+cote);
    glVertex3f(o.getx(), o.gety()+cote, o.getz()+cote);

    glVertex3f(o.getx(), o.gety()+cote, o.getz());
    glVertex3f(o.getx()+cote, o.gety()+cote, o.getz());

    glVertex3f(o.getx(), o.gety(), o.getz()+cote);
    glVertex3f(o.getx()+cote, o.gety(), o.getz()+cote);

    glVertex3f(o.getx(), o.gety()+cote, o.getz()+cote);
    glVertex3f(o.getx()+cote, o.gety()+cote, o.getz()+cote);


  glEnd();

}



struct poly{
  int nbP;
  Point * pts;
};
struct off{
  int nbPl;
  poly * pp;
};

off parseOFF(string filename){

  string line;
  ifstream in(filename.c_str());

  getline(in,line);
  if(line != "OFF") {
    printf("not a OFF file\n");
    exit(-1);
  }
  int a,b,c;
  getline(in,line);
  a=line.find(" ",0);
  int nbPs=atoi(line.substr(0,a+1).c_str());
  b=line.find(" ",a);
  int nbPl=atoi(line.substr(a,b+1).c_str());

  Point * ps = new Point[nbPs];
  poly * pls = new poly[nbPl];

  for (int i =0;i<nbPs;i++){
    getline(in,line);
    a=line.find(" ",0);
    b=line.find(" ",a+1);
    c=line.find(" ",b+1);

    float x=atof(line.substr(0,a).c_str());
    float y=atof(line.substr(a+1,b).c_str());
    float z=atof(line.substr(b+1,c).c_str());
    ps[i]= * new Point (x,y,z);
  }
  for (int i =0;i<nbPl;i++){
    getline(in,line);

    a=line.find(" ",0);
    int j=atoi(line.substr(0,a+1).c_str());
    pls[i].nbP=j;
    pls[i].pts= new Point [j];
    int p1,p2;
    p1=a+1;
    for(int k =0;k<j;k++){
      p2=line.find(" ",p1);
      pls[i].pts[k]=ps[atoi(line.substr(p1,p2+1).c_str())];
      p1=p2+1;
    }
  }

  off of;
  of.nbPl=nbPl;
  of.pp=pls;
  return of;
}

void drawOff(off ppp,double * color ){
  glColor3f(color[0],color[1],color[2]);
  for(int i =0;i<ppp.nbPl;i++){
    glBegin(GL_POLYGON);
    for(int j=0;j<ppp.pp[i].nbP;j++){
      glVertex3f(ppp.pp[i].pts[j].getx(),ppp.pp[i].pts[j].gety(),ppp.pp[i].pts[j].getz());
    }
    glEnd();
  }

}


Point * CalculHelice(Point centre,double rayon,double hauteur,int u){

Point * ps = new Point[u];
double angle=((2*PI)/u);


  for(unsigned int i=0; i < u; i++) {
    double x=(rayon * cos(angle * i));
    double y=(rayon * sin(angle * i));
    ps[i]=* new Point(centre.getx()+x, centre.gety()+y ,centre.getz()+hauteur*i/u);
    //angle+=angle;
  }
return ps;
}
Point * CalculHelice(Point centre,double rayon,double hauteur,int rotations,int u){

Point * ps = new Point[u];
double angle=((2*PI)/u);

  for(unsigned int i=0; i < u; i++) {
    double x=(rayon * cos(angle * i*rotations));
    double y=(rayon * sin(angle * i*rotations));
    ps[i]=* new Point(centre.getx()+x, centre.gety()+y ,centre.getz()+hauteur*i/u);
    //angle+=angle;
  }
return ps;
}
//////////////////////////////////////////////////////////////////////////////////////////
// Fonction que vous allez modifier afin de dessiner
/////////////////////////////////////////////////////////////////////////////////////////

void render_scene(){
  //glColor3f(1.0,0.0,0.0);
  //grid();
  gluLookAt(
    sin(u*2*M_PI) , tan(v * 2.0 * M_PI), cos(u*2*M_PI)/2 ,
    0.0, 0.0, 0.0,
    0.0, 0.0, 1.0
  );

// int mrds=40;
// DrawSommetsCyl_Con(CalculSommetsCylindre(* new Point(0,0,0),5.0,10.0,nmed),nmed);
//
// DrawSommetsCyl_Con(CalculSommetsCone(* new Point(0,0,0),5.0,10.0,nmed),nmed);
//
 DrawSphere(* new Point(0,0,3), 3.0 , nmed,npar) ;
int uu=30;
Point * ps=CalculHelice(* new Point(0,0,0),5.0,6.0,2,uu);
glColor3f(1.0,0.0,1.0);
glPointSize(6);
for(int i = 0;i<uu;i++){
  glBegin(GL_POINTS);
  //printf("%f %f %f\n",ps[i].getx(),ps[i].gety(),ps[i].getz());
  glVertex3f(ps[i].getx(),ps[i].gety(),ps[i].getz());
  glEnd();
}








}
