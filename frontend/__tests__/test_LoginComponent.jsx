import React from 'react';
import {shallow,mount} from 'enzyme';
import moxios from 'moxios';
import LoginComponent from '../app/LoginComponent.jsx';
import sinon from 'sinon';

describe("LoginComponent", ()=>{
    beforeEach(()=>moxios.install());
    afterEach(()=>moxios.uninstall());

    test("if not logged in, should render login dialog", ()=>{
        const logInCallback = sinon.spy();
        const logOutCallback = sinon.spy();

        const rendered = shallow(<LoginComponent onLoggedIn={logInCallback} onLoggedOut={logOutCallback}
                                                 currentlyLoggedIn={false}/>);

        const dlg = rendered.find('div.inline-dialog');
        expect(dlg.find('h2.inline-dialog-title').text()).toEqual("Login");
        expect(dlg.find('input').length).toEqual(2);
        expect(dlg.find('button').text()).toEqual("Log In");
    });

    test("if logged in, should show banner text and a logout button", ()=>{
        const logInCallback = sinon.spy();
        const logOutCallback = sinon.spy();

        const rendered = shallow(<LoginComponent onLoggedIn={logInCallback} onLoggedOut={logOutCallback}
                                                 currentlyLoggedIn={true} username="Gumby"/>);

        const dlg = rendered.find('div.inline-dialog');
        expect(dlg.find('h2.inline-dialog-title').text()).toEqual("Login");
        expect(dlg.find('input').length).toEqual(0);
        /* the odd spacing is because the actual content contains markup which uses margins to space out */
        expect(dlg.find('p.inline-dialog-content').text()).toEqual("You are currently logged in asGumby");
        expect(dlg.find('button').text()).toEqual("Log out");
    });

    test("clicking login should pass entered credentials to the server to request login, and notify callback", (done)=>{
        const logInCallback = sinon.spy();
        const logOutCallback = sinon.spy();

        const rendered = shallow(<LoginComponent onLoggedIn={logInCallback} onLoggedOut={logOutCallback}
                                                 currentlyLoggedIn={false}/>);

        const usernameBox = rendered.find('input#username');
        const passwordBox = rendered.find('input#password');
        usernameBox.simulate("change",{target: {value: "fred"}});
        passwordBox.simulate("change",{target: {value: "ihavealongpassword"}});

        const loginButton = rendered.find('button');
        loginButton.simulate("click");

        return moxios.wait(()=>{
            const request = moxios.requests.mostRecent();
            try {
                expect(request.url).toEqual("/api/login");
                expect(request.config.data).toEqual(JSON.stringify({username: "fred", password: "ihavealongpassword"}));
            } catch(err){
                done.fail(err);
            }

            request.respondWith({
                status: 200,
                response: {status: "ok",detail: "logged in",uid: "fred"}
            }).then(()=>{
                expect(logInCallback.calledOnce).toBeTruthy();
                expect(rendered.instance().state.loading).toBeFalsy();
                expect(rendered.instance().state.enteredUserName).toEqual("");
                expect(rendered.instance().state.enteredPassword).toEqual("");

                done();
            }).catch(error=>{
                console.error(error);
                done.fail(error);
            })
        })
    });

    test("clicking logout should message the server to request logout, and notify callback", (done)=>{
        const logInCallback = sinon.spy();
        const logOutCallback = sinon.spy();

        const rendered = shallow(<LoginComponent onLoggedIn={logInCallback} onLoggedOut={logOutCallback}
                                                 currentlyLoggedIn={true} username="kevin"/>);

        const logoutButton = rendered.find('button');
        logoutButton.simulate("click");

        return moxios.wait(()=>{
            const request = moxios.requests.mostRecent();
            try {
                expect(request.url).toEqual("/api/logout");
            } catch(err){
                done.fail(err);
            }

            request.respondWith({
                status: 200,
                response: {status: "ok",detail: "logged out"}
            }).then(()=>{
                expect(logOutCallback.calledOnce).toBeTruthy();
                expect(rendered.instance().state.loading).toBeFalsy();
                expect(rendered.instance().state.enteredUserName).toEqual("");
                expect(rendered.instance().state.enteredPassword).toEqual("");

                done();
            }).catch(error=>{
                console.error(error);
                done.fail(error);
            })
        })
    })
});