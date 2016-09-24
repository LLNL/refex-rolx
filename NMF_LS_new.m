%
% Version: 1.0
% Author: Hanghang Tong
% Contact: keith@llnl.gov
%

function [F,G,c1,loss_all] = NMF_LS_new(V,niter,r,type,F0,G0)
%--------------------------------------------------------------------------
%  Using Lee and Seung's algorithm for NMF
%
%  Arguments:
%  V: nxd data matrix
%  type: 0: square loss; 1: KL loss (default = 0)
%  F0: initial F matrix (default = random)
%  G0: initial G matrix (default = random)
%  
%  Returns:
%  F: dxr cluster center matrix 
%  G: nxr cluster assignment matrix
%  c1: cluster assignments
%  loss_all: error of selected model
%--------------------------------------------------------------------------
if min(min(V)) < 0
    error('matrix entries can not be negative');
end
V = V';
% mkdir(filename);

[d,n] = size(V);
if nargin<4
    type = 0;
end
if nargin<3
    r = 5;
end
if nargin<2
    niter = 1000;
end
mx = max(max(V));
if nargin<6
    G0 = rand(n,r)*mx;
end
if nargin<5
    F0 = rand(d,r)*mx;
end
%niter = 1000;

err_eps = 1e-5; 
epsilon = 1e-27;

G = G0;
F = F0;

clear G0 F0;

Gold = G;
Fold = F;

if r >= d
    G = V';
    F = eye(d);
    loss = 0;
elseif r >= n
    G = eye(n);
    F = V;
    loss = 0;
else
    for i=1:niter
        G = G.*((V'*F)./(G*(F'*F)+epsilon));
        Gloss = sum(sum((G-Gold).^2));
        Gold = G;

        F = F.*((V*G)./(F*(G'*G)+epsilon));
        Floss = sum(sum((F-Fold).^2));
        Fold = F;

        loss = sqrt(eval_Fro_loss(V,F,G));
        if abs(Gloss + Floss) < err_eps

            break;
        end
    end
end 

%disp([r i loss abs(loss-old_loss)]);


loss_all(1) = loss;
a = 1./max(sum(G,2),1E-20);
G_norm = sparse(1:n,1:n,a,n,n) * G;
G_norm = G_norm+epsilon;
loss_all(2) = -sum(sum(G_norm.*log2(G_norm)));



F = F';

% normalize F
N = diag(1./max(sum(F,2), 1E-20));
F = N * F;
G = G * inv(N);

%find cluster memeber ship
%by max
c1 = zeros(n,1);
for i=1:n
    id = find(G(i,:)==max(G(i,:)));
    if numel(id) > 0
        c1(i,1) = id(1); 
    else
        c1(i,1) = 0;
    end
end


function loss = eval_Fro_loss(V,F,G)
if ~issparse(V)
    temp = V-F*G';
    loss = sum(temp(:).^2);
else
    ind = find(V);
    U = F*G';
    loss = sum(V(ind).^2)+sum(U(:).^2)-2*V(ind)'*U(ind);
end
    
