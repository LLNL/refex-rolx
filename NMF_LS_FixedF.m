%
% Version: 1.0
% Author: Keith Henderson
% Contact: keith@llnl.gov
%

function G = NMF_LS_FixedF(V, F, niter, G0)
%--------------------------------------------------------------------------
%  Using Lee and Seung's algorithm for NMF
%  Arguments:
%  V: nxd data matrix
%  F: dxr cluster center matrix 
%  niter: number of iterations
%  G0: initial G matrix (default = random)
%  
%  Returns:
%  G: nxr cluster assignment matrix
%--------------------------------------------------------------------------
if min(min(V)) < 0
    error('matrix entries can not be negative');
end
V = V';
% mkdir(filename);

[d,n] = size(V);
r = size(F,1);

F = F';

if nargin<3
    niter = 1000;
end
mx = max(max(V));

if nargin<4
    G0 = rand(n,r)*mx;
end
%niter = 1000;

err_eps = 1e-5; 
epsilon = 1e-27;

G = G0;

clear G0 F0;

Gold = G;
%Fold = F;



for i=1:niter
    G = G.*((V'*F)./(G*(F'*F)+epsilon));
    Gloss = sum(sum((G-Gold).^2));
    Gold = G;

    %F = F.*((V*G)./(F*(G'*G)+epsilon));
    %Floss = sum(sum((F-Fold).^2));
    %Fold = F;

    loss = sqrt(eval_Fro_loss(V,F,G));
    if abs(Gloss) < err_eps

        break;
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
% N = diag(1./max(sum(F,2), 1E-20));
% F = N * F;
% G = G * inv(N);

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
    
