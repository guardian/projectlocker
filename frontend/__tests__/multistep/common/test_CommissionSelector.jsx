import React from 'react';
import {shallow,mount} from 'enzyme';
import ErrorViewComponent from '../../../app/multistep/common/CommissionSelector.jsx';
import sinon from 'sinon';
import assert from 'assert';
import moxios from 'moxios';
import CommissionSelector from "../../../app/multistep/common/CommissionSelector";

describe("CommissionSelector", ()=>{
    beforeEach(()=>moxios.install());
    afterEach(()=>moxios.uninstall());

    it("should load data from the server on mount and present a list of options", (done)=>{
        const valueSetSpy = sinon.spy();
        const rendered = shallow(<CommissionSelector workingGroupId={4}
                                                     selectedCommissionId={2}
                                                     showStatus="In production"
                                                     valueWasSet={valueSetSpy}/>);

        return moxios.wait(()=>{
            const req = moxios.requests.mostRecent();
            try{
                expect(req.config.url).toEqual("/api/pluto/commission/list?length=150");
                expect(req.config.data).toEqual(JSON.stringify({workingGroupId: 4, status:"In production", match:"W_EXACT"}))
            } catch(err) {
                done.fail(err);
            }
            req.respondWith({
                status: 200,
                response: {result:[
                    {id: 1, collectionId: 1234, siteId: "VX", title:"Keith"},
                    {id: 2, collectionId: 2345, siteId: "VX", title:"Jen"},
                    {id: 3, collectionId: 3456, siteId: "VX", title:"Sarah"}
                ],status:"ok"}
            }).then(()=>{
                rendered.update();
                const selectorBox = rendered.find('FilterableList');
                expect(selectorBox.props().unfilteredContent).toEqual([
                    { name: "Keith", value: 1},
                    { name: "Jen", value: 2},
                    { name: "Sarah", value: 3}
                ]);

                expect(selectorBox.props().value).toEqual(2);
                done();
            }).catch(err=>done.fail(err));
        })
    });

    it("should present an error if download fails", (done)=>{
        const valueSetSpy = sinon.spy();
        const rendered = shallow(<CommissionSelector workingGroupId={4}
                                                   selectedCommissionId={2}
                                                   showStatus="In production"
                                                   valueWasSet={valueSetSpy}/>);

        return moxios.wait(()=>{
            const req = moxios.requests.mostRecent();
            try{
                expect(rendered.instance().state.error).toBeFalsy();
                expect(req.config.url).toEqual("/api/pluto/commission/list?length=150");
                expect(req.config.data).toEqual(JSON.stringify({workingGroupId: 4, status:"In production", match:"W_EXACT"}))
            } catch(err) {
                done.fail(err);
            }
            req.respondWith({
                status: 500,
                response: {detail:"hanungah",status:"error"}
            }).then(()=>{
                expect(rendered.instance().state.error).toBeTruthy();
                done();
            }).catch(err=>done.fail(err));
        })
    });

    it("should notify the callback when the value changes", (done)=>{
        const valueSetSpy = sinon.spy();
        const rendered = shallow(<CommissionSelector workingGroupId={4}
                                                   selectedCommissionId={2}
                                                   showStatus="In production"
                                                   valueWasSet={valueSetSpy}/>);

        return moxios.wait(()=>{
            const req = moxios.requests.mostRecent();
            try{
                expect(req.config.url).toEqual("/api/pluto/commission/list?length=150");
                expect(req.config.data).toEqual(JSON.stringify({workingGroupId: 4, status:"In production", match:"W_EXACT"}))
            } catch(err) {
                done.fail(err);
            }
            req.respondWith({
                status: 200,
                response: {result:[
                    {id: 1, collectionId: 1234, siteId: "VX", title:"Keith"},
                    {id: 2, collectionId: 2345, siteId: "VX", title:"Jen"},
                    {id: 3, collectionId: 3456, siteId: "VX", title:"Sarah"}
                ],status:"ok"}
            }).then(()=>{
                rendered.update();
                const selectorBox = rendered.find('FilterableList');
                selectorBox.props().onChange("3");
                assert(valueSetSpy.calledWith(3));
                done();
            }).catch(err=>done.fail(err));
        })
    })
});