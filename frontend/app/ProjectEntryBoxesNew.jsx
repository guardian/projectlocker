import React from 'react';
import PropTypes from 'prop-types';
import axios from 'axios';
import ProjectEntryBox from "./ProjectEntryBox.jsx";
import ProjectEntryFilterComponent from "./filter/ProjectEntryFilterComponent.jsx";

class ProjectEntryBoxes extends React.Component {
    ITEM_LIMIT = 50
    static propTypes = {
        title: PropTypes.string.isRequired
    }

    constructor(props) {
        super(props);

        this.endpoint = '/api/project';
        this.filterEndpoint = '/api/project/list';
        this.pageSize = 5;

        this.state = {
            filterTerms: {
                match: "W_ENDSWITH"
            },
            currentPage: 0,
            interfaceSize: 0,
            data: []
        }
        this.filterDidUpdate = this.filterDidUpdate.bind(this);
    }

    changeSize() {
        this.setState(oldState=>{return {interfaceSize: oldState.interfaceSize===0 ? 1 : 0 }});
    }

    dependenciesDidLoad() {

    }

    /* called when we receive data; can be over-ridden by a subclass to do something more clever */
    gotDataCallback(response, cb){
        this.setState({
            data: this.state.data.concat(response.data.result)
        }, cb);
    }

    componentDidMount() {
        this.loadDependencies().then(()=>{
            this.dependenciesDidLoad();
            this.reload();
        });
    }

    /* reloads the data for the component based on the endpoint configured in the constructor */
    reload(){
        this.setState({
            currentPage: 0,
            data: []
        }, ()=> this.getNextPage());
    }

    /* loads the next page of data */
    getNextPage() {
        const startAt = this.state.currentPage * this.pageSize;
        const length = this.pageSize;

        const axiosFuture = this.filterEndpoint ?
            axios.put(this.filterEndpoint + "?startAt=" + startAt + "&length=" + length, this.state.filterTerms) :
            axios.get(this.endpoint + "?startAt=" + startAt + "&length=" + length);

        axiosFuture.then(response=>{
            this.setState({
                currentPage: this.state.currentPage+1
            }, ()=>{
                this.gotDataCallback(response, ()=> {
                    if (response.data.result.length > 0)
                        if(this.pageSize*this.state.currentPage>=ProjectEntryBoxes.ITEM_LIMIT)
                            this.setState({maximumItemsLoaded: true});
                        else
                            this.getNextPage()
                });
            })
        }).catch(error=>{
            console.error(error);
        });
    }

    loadDependencies(){
        return new Promise((accept,reject)=>axios.get("/api/isLoggedIn")
            .then(response=>{
                if(response.data.status==="ok")
                    this.setState({isAdmin: response.data.isAdmin, uid: response.data.uid}, ()=>accept());
            })
            .catch(error=>{
                if(this.props.error.response && this.props.error.response.status===403)
                    this.setState({isAdmin: false}, ()=>accept());
                else {
                    console.error(error);
                    this.setState({isAdmin: false, error: error}, ()=>reject());
                }
            })
        );
    }

    /* this can be referenced from a filter component in a subclass and should be called to update the active filtering.
    this will cause a reload of data from the server
    */
    filterDidUpdate(newterms){
        this.setState({filterTerms: newterms},()=>this.reload());
    }

    itemLimitWarning(){
        if(this.state.maximumItemsLoaded)
            return <p className="warning-text"><i className="fa-info fa" style={{marginRight: "0.5em", color: "orange"}}/>Maximum of {GeneralBoxComponent.ITEM_LIMIT} items have been loaded. Use filters to narrow this down.</p>
        else
            return <p style={{margin: 0}}/>
    }

    render() {
        console.log(this.state.data);
        return <div>
            <span className="list-title"><h2 className="list-title">{this.props.title}</h2></span>
            <ProjectEntryFilterComponent filterTerms={this.state.filterTerms} filterDidUpdate={this.filterDidUpdate}/>
            {this.itemLimitWarning()}

            <span className="banner-control">
                    <button id="newElementButton" onClick={this.newElementCallback}>New</button>
                    <button className="size_button" onClick={this.changeSize.bind(this)}>Change Interface Size</button>
                </span>

            {
            this.state.data.map((entry,idx)=> {
                console.log(entry);
                console.log(idx);
                return <ProjectEntryBox key={idx} interfaceSize={this.state.interfaceSize} projectId={entry.id}
                                 projectTypeId={entry.projectTypeId} projectTitle={entry.title}
                                 projectOwner={entry.user}/>
            })
            }
        </div>
    }
}

export default ProjectEntryBoxes;