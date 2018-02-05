import React from 'react';
import {shallow,mount} from 'enzyme';
import moxios from 'moxios';
import GeneralDeleteComponent from '../../app/delete/GeneralDeleteComponent.jsx';
import sinon from 'sinon';
import assert from 'assert';

describe("GeneralDeleteComponent", ()=>{
    beforeEach(() => moxios.install());
    afterEach(() => moxios.uninstall());

    it("should download content when mounted and render a summary", (done)=>{
        const rendered=shallow(<GeneralDeleteComponent match={{params: {itemid: 9}}}/>);
        rendered.instance().itemClass="long bent thing with a sort of lump on the end";
        const summarySpy = sinon.spy();

        rendered.instance().getSummary = summarySpy;

        return moxios.wait(()=>{
            expect(moxios.requests.mostRecent().config.url).toEqual('/api/unknown/9');
            let request = moxios.requests.mostRecent();
            request.respondWith({
                status: 200,
                response: {'status': 'ok','result': {name: 'something', data: 'something else'}}
            }).then(() => {
                rendered.update();
                expect(rendered.find('h3').text()).toEqual("Delete long bent thing with a sort of lump on the end");
                expect(rendered.find('p.information').text()).toBeTruthy();
                assert(summarySpy.calledOnce);
                done();
            }).catch(error=>{
                console.error(error);
                done(error);
            })
        });
    });

    it("should call the delete endpoint when the delete button is clicked", (done)=>{
        const rendered=mount(<GeneralDeleteComponent match={{params: {itemid: 9}}}/>);
        rendered.instance().itemClass="long bent thing with a sort of lump on the end";

        return moxios.wait(()=>{
            const req = moxios.requests.at(0);

            expect(req.config.url).toEqual('/api/unknown/9');
            expect(req.config.method).toEqual('get');

            req.respondWith({
                status: 200,
                response: {'status': 'ok','result': {name: 'something', data: 'something else'}}
            }).then(() => {
                rendered.update();
                rendered.find('button#deleteButton').simulate("click");

                return moxios.wait(()=>{
                    const deleteRequest = moxios.requests.at(1);
                    expect(deleteRequest.config.url).toEqual('/api/unknown/9');
                    expect(deleteRequest.config.method).toEqual('delete');
                    deleteRequest.respondWith({
                        status: 200
                    }).then(()=>{
                        done();
                    }).catch(error=>{
                        console.error(error);
                        done(error);
                    });
                });
            }).catch(error=>{
                console.error(error);
                done(error);
            });
        });
    })
});